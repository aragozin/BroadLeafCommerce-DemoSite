package info.ragozin.loadscript;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class HeatClinicBot {
	
	private final String url;
	private final WebDriver driver;
	
	public HeatClinicBot(String url, WebDriver driver) {
		this.url = url;
		this.driver = driver;
	}
	
	public void openLandingPage() {
		driver.get(url);
		
		System.out.println("NOW AT " + driver.getCurrentUrl());
	}

	public void siteSearch(String query) {

		driver.findElement(By.cssSelector("input.js-search")).sendKeys(query);
		driver.findElement(By.cssSelector("input.js-search")).sendKeys("\n");

		System.out.println("NOW AT " + driver.getCurrentUrl());
	}
	
//	public void openRandom()
}
