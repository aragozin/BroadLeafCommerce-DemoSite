package info.ragozin.loadscript;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class CheckoutPage {

	private final WebDriver driver;

	public CheckoutPage(WebDriver driver) {
		this.driver = driver;
	}

	public void waitPaymentCardToShow() {
		long deadline = System.currentTimeMillis() + 30000;
		while(!driver.findElement(By.cssSelector("#payment_methods")).isDisplayed()) {
			if (System.currentTimeMillis() > deadline) {
				throw new RuntimeException("Timeout waiting for PAYMENT card");
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
	}
	
	public void expectShipmentCardVisible(boolean visible) {
		WebElement we = driver.findElement(By.cssSelector("#shipping_info_stage"));
		if (we.isDisplayed() != visible) {
			Assert.fail("SHIPPING stage should be " + (visible ? "visible" : "hidden"));
		}
	}

	public void expectPaymentCardVisible(boolean visible) {
		WebElement we = driver.findElement(By.cssSelector("#payment_methods"));
		if (we.isDisplayed() != visible) {
			Assert.fail("PAYMENT stage should be " + (visible ? "visible" : "hidden"));
		}
	}

	public void setFullName(String name) {
		driver.findElement(By.cssSelector("#fullName")).sendKeys(name);
	}

	public void setAddress(String addr) {
		driver.findElement(By.cssSelector("#addressLine1")).sendKeys(addr);
	}

	public void setAddress2(String addr) {
		driver.findElement(By.cssSelector("#addressLine2")).sendKeys(addr);
	}

	public void setCity(String city) {
		driver.findElement(By.cssSelector("#city")).sendKeys(city);		
	}
	
	public void setShippingMethod(int method) {
		driver.findElements(By.cssSelector("div.shipping-methods-wrapper input")).get(method).click();
	}

	public void setState(String state) {
		driver.findElement(By.cssSelector("#stateProvinceRegion")).sendKeys(state + "\n");		
	}

	public void setPostalCode(String code) {
		driver.findElement(By.cssSelector("#postalCode")).sendKeys(code);		
	}

	public void stageContinue() {
		for(WebElement we: driver.findElements(By.cssSelector("div.checkout-stage-action .btn-primary"))) {
			if (we.isDisplayed()) {
				we.click();
				return;
			}
		}
	}
	
	public void placeOrder() {
		driver.findElement(By.cssSelector("div.checkout-stage-action .js-performCheckout")).click();
	}
	
	public void chooseCollectOnDeliveryPayment() {
		int tries = 2;
		while(tries > 0) {
			WebElement codButton = driver.findElement(By.cssSelector("div.payment-method-selectors a[href='#COD']"));
			try {
				codButton.click();
				return;
			}
			catch(ElementNotInteractableException e) {
				if (--tries == 0) {
					throw e;
				}
				else {
					continue;
				}
			}
		}
	}
}
