package info.ragozin.loadscript;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.interactions.Actions;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.javascript.StrictErrorReporter;
import com.gargoylesoftware.htmlunit.javascript.host.css.CSSStyleSheet;

public class LoadScriptCheck {

	HtmlUnitErrorHandler errorHandler = new HtmlUnitErrorHandler();
	WebDriver driver;
	
	@Before
	public void init() {
		
		java.util.logging.Logger.getLogger(CSSStyleSheet.class.getName()).setLevel(Level.OFF);
		java.util.logging.Logger.getLogger(StrictErrorReporter.class.getName()).setLevel(Level.OFF);
		
		HtmlUnitDriver driver = new HtmlUnitDriver(true) {
			 
			protected WebClient modifyWebClient(WebClient client) {
				client.setIncorrectnessListener(errorHandler);
				client.setCssErrorHandler(errorHandler);
				client.setJavaScriptErrorListener(errorHandler);
//				client.setHTMLParserListener(errorHandler);
//				client.setWebConnection(new TrivialCachingWebConnection(new TracingWebConnection(client.getWebConnection())));
				client.setWebConnection(new TrivialCachingWebConnection(new DumpingWebConnection("target/trace/trace-", client.getWebConnection())));
				
				return client;
			};

			
		};
		
		this.driver = driver;
	}

	@Test
	public void singlePurchase() throws InterruptedException {
			makePurchaseAsAGuest();
	}
	
	@Test
	public void spamPurchases() throws InterruptedException {
		while(true) {
			makePurchaseAsAGuest();
		}
	}

	@Test
	public void spamPurchasesMP() throws InterruptedException {
		int tcount = 3;
		Executor exec = Executors.newFixedThreadPool(tcount);
		for(int i = 0; i != tcount; ++i) {
			exec.execute(new Runnable() {
				@Override
				public void run() {
					LoadScriptCheck client = new LoadScriptCheck();
					client.init();
					while(true) {
						client.makePurchaseAsAGuest();
					}
				}
			});
		}
		while(true) {
			Thread.sleep(1000);
		}
	}
	
	public void makePurchaseAsAGuest() {
		driver.get("http://localhost:8080/");
		
		System.out.println("NOW AT " + driver.getCurrentUrl());
		
		driver.findElement(By.cssSelector("input.js-search")).sendKeys("Hot");
		driver.findElement(By.cssSelector("input.js-search")).sendKeys("\n");

		System.out.println("NOW AT " + driver.getCurrentUrl());

		clickProductPreview(1);
		
		System.out.println("NOW AT " + driver.getCurrentUrl());
		
		clickAddToCardOnPreviewPopup();		

		System.out.println("NOW AT " + driver.getCurrentUrl());

//		clickProductPreview(0);
//
//		System.out.println("NOW AT " + driver.getCurrentUrl());
		
		openCartPage();
		
		System.out.println("NOW AT " + driver.getCurrentUrl());		
		
		checkoutAsGuestFromCartPage();

		System.out.println("NOW AT " + driver.getCurrentUrl());
		
		CheckoutPage page = new CheckoutPage(driver);
		
		page.expectShipmentCardVisible(true);
		page.expectPaymentCardVisible(false);
		
		page.setFullName("Tom");
		page.setAddress("Nowhere");
		page.setShippingMethod(1);
		page.setCity("Town");
		page.setState("WY");
		page.stageContinue();

		System.out.println("NOW AT " + driver.getCurrentUrl());

		page.waitPaymentCardToShow();

		page.chooseCollectOnDeliveryPayment();
		page.stageContinue();

		System.out.println("NOW AT " + driver.getCurrentUrl());
		
		page.placeOrder();

		System.out.println("NOW AT " + driver.getCurrentUrl());
	}

	private void checkoutAsGuestFromCartPage() {
		driver.findElement(By.cssSelector("div.cart-actions a.btn-primary")).click();
		
		driver.findElement(By.cssSelector("div.checkout-guest-wrapper button.btn-primary")).click();
	}

	private void openCartPage() {
		
		waitForElement("a.goto-full-cart");

		// force show cart popup
		((HtmlUnitDriver)driver).executeScript("$(\".js-miniCart\").addClass(\"open\")");
		
		WebElement btn = driver.findElement(By.cssSelector("a.goto-full-cart"));
		if (!btn.isDisplayed()) {
			new String();
		}
		btn.click();
	}

	private WebElement waitForElement(String selector) {
		long deadline = System.currentTimeMillis() + 30000;
		while(driver.findElements(By.cssSelector(selector)).isEmpty()) {
			if (System.currentTimeMillis() > deadline) {
				throw new RuntimeException("Timeout waiting for element: " + selector);
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
		return driver.findElement(By.cssSelector(selector));
	}

	private void clickAddToCardOnPreviewPopup() {
		waitForElement(".modal-dialog button.js-addToCart");
		driver.findElement(By.cssSelector(".modal-dialog button.js-addToCart")).click();
	}

	private void clickProductPreview(int n) {
		List<WebElement> elements = driver.findElements(By.cssSelector("div.card-product"));
		
		WebElement card = elements.get(n);
		
		String productId = card.getAttribute("data-id");
		System.out.println("Matching product: " + productId);
		String selector = "div.card-product[data-id='" + productId + "'] ";

		waitForElement(selector + "div.card-image");
		
		new Actions(driver)
			.moveToElement(driver.findElement(By.cssSelector(selector + "div.card-image")))
			.pause(100)
			.click(driver.findElement(By.cssSelector(selector + "button.btn-quickview")))
			.pause(300) // animation
			.perform();		
	}
	
}
