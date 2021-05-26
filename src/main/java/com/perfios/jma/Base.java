package com.perfios.jma;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import io.github.bonigarcia.wdm.WebDriverManager;

public class Base {
	
static WebDriver driver;
	
	static {
		
		WebDriverManager.chromedriver().setup();
		driver = new ChromeDriver();
		
	}

}
