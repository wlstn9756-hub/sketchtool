// 
// Decompiled by Procyon v0.6.0
// 

package com.devco;

import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.Alert;
import java.util.Iterator;
import java.util.List;
import org.openqa.selenium.JavascriptExecutor;
import api.Api;
import java.util.function.Function;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import org.openqa.selenium.By;
import util.SysUtil;
import org.openqa.selenium.WebDriver;
import util.Util;
import config.Config;
import java.net.URISyntaxException;
import java.io.IOException;
import model.NaverAccount;
import org.openqa.selenium.chrome.ChromeDriver;

public class NBlogPostDelete
{
    private final ChromeDriver chromeDriver;
    private NaverAccount naverAccount;
    
    public NBlogPostDelete(final ChromeDriver chromeDriver, final NaverAccount naverAccount) {
        this.chromeDriver = chromeDriver;
        this.naverAccount = naverAccount;
    }
    
    public boolean runNBlogPostDelete() throws IOException, URISyntaxException {
        final boolean loginSuccess = this.login();
        if (!loginSuccess) {
            return false;
        }
        try {
            return this.goToBlogPost();
        }
        catch (final Exception e) {
            e.printStackTrace();
            System.out.println("\ube14\ub85c\uadf8 \ubc1c\ud589 \uc911 \uc5d0\ub7ec \ubc1c\uc0dd\uc73c\ub85c \ud574\ub2f9 \ubc1c\ud589\uac74\uc740 \ucde8\uc18c \ucc98\ub9ac\ub85c \uc800\uc7a5");
            return false;
        }
    }
    
    private boolean login() throws IOException {
        this.chromeDriver.get(Config.NAVER_LOGIN_URL);
        Util.Sleep(1500, "\ub85c\uadf8\uc778 \ud654\uba74\uc73c\ub85c \uc774\ub3d9\ud569\ub2c8\ub2e4.");
        Util.waitForPageLoad(this.chromeDriver, 180);
        final JavascriptExecutor js = this.chromeDriver;
        js.executeScript("document.getElementById('id').value='" + this.naverAccount.getId() + "';", new Object[0]);
        js.executeScript("document.getElementById('pw').value='" + this.naverAccount.getPw() + "';", new Object[0]);
        Util.Sleep(1000, "\uc544\uc774\ub514/\ube44\ubc88 \uc785\ub825\uc911");
        if (SysUtil.isWindows()) {
            this.chromeDriver.findElement(By.xpath("//*[@id=\"keep\"]")).click();
            Util.Sleep(2000, "\ub85c\uadf8\uc778 \uc0c1\ud0dc \uc720\uc9c0 \uccb4\ud06c\ubc15\uc2a4 \ud074\ub9ad");
            final WebElement checkbox = this.chromeDriver.findElement(By.id("switch"));
            if (checkbox.isSelected()) {
                final WebElement label = this.chromeDriver.findElement(By.cssSelector("label.switch_btn"));
                label.click();
            }
            Util.Sleep(2000, "IP \ubcf4\uc548 \ud574\uc81c");
        }
        this.chromeDriver.findElement(By.id("log.login")).click();
        Util.Sleep(1500, "\ub85c\uadf8\uc778 \ubc84\ud2bc \ud074\ub9ad");
        Util.waitForPageLoad(this.chromeDriver, 180);
        try {
            final WebDriverWait wait = new WebDriverWait(this.chromeDriver, Duration.ofSeconds(10L));
            final WebElement logoutButton = ((FluentWait<Object>)wait).until((Function<? super Object, WebElement>)ExpectedConditions.visibilityOfElementLocated(By.className("MyView-module__btn_logout___bsTOJ")));
            System.out.println("\u2705 \ub85c\uadf8\uc778 \uc131\uacf5: \ub85c\uadf8\uc544\uc6c3 \ubc84\ud2bc\uc774 \ubcf4\uc785\ub2c8\ub2e4.");
            return true;
        }
        catch (final Exception e) {
            System.out.println("\u274c \ub85c\uadf8\uc778 \uc2e4\ud328: \ub85c\uadf8\uc544\uc6c3 \ubc84\ud2bc\uc744 \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.");
            this.chromeDriver.manage().deleteAllCookies();
            if (isAccountProtected(this.chromeDriver)) {
                System.out.println("\ud83d\udeab \ubcf4\ud638\uc870\uce58\uac00 \uac10\uc9c0\ub418\uc5b4 \uc791\uc5c5\uc744 \uc911\ub2e8\ud569\ub2c8\ub2e4.");
                final Api api = new Api();
                api.updateNaverAccountStatus(this.naverAccount.getId(), "inactive");
                return false;
            }
            return false;
        }
    }
    
    public static boolean isAccountProtected(final WebDriver driver) {
        final List<WebElement> links = driver.findElements(By.xpath("//a[text()='\ubcf4\ud638\uc870\uce58 \ud574\uc81c']"));
        if (!links.isEmpty()) {
            return true;
        }
        final List<WebElement> titles = driver.findElements(By.className("top_title"));
        for (final WebElement title : titles) {
            final String text = title.getText();
            if (text.contains("\ube44\uc815\uc0c1\uc801\uc778 \ud65c\ub3d9\uc774 \uac10\uc9c0\ub418\uc5b4")) {
                return true;
            }
        }
        return false;
    }
    
    private boolean goToBlogPost() {
        Util.waitForPageLoad(this.chromeDriver, 180);
        this.chromeDriver.get(this.naverAccount.getBlogUrl());
        Util.Sleep(2000, "\ube14\ub85c\uadf8 \ud3ec\uc2a4\ud2b8 \uc774\ub3d9 \uc911");
        Util.waitForPageLoad(this.chromeDriver, 180);
        try {
            final WebDriverWait wait = new WebDriverWait(this.chromeDriver, Duration.ofSeconds(15L));
            ((FluentWait<Object>)wait).until((Function<? super Object, Object>)ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("mainFrame")));
            System.out.println("\u2705 mainFrame iframe \uc9c4\uc785 \uc131\uacf5");
            final WebElement container = ((FluentWait<Object>)wait).until((Function<? super Object, WebElement>)ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.area_btn_postedit.pcol2")));
            this.chromeDriver.executeScript("arguments[0].scrollIntoView({block: 'center'});", container);
            Thread.sleep(500L);
            final WebElement deleteLink = container.findElement(By.xpath(".//a[normalize-space(text())='\uc0ad\uc81c']"));
            deleteLink.click();
            Util.Sleep(2500, "\uc0ad\uc81c \ubc84\ud2bc \ud074\ub9ad \ud6c4 \ud655\uc778\uc6a9 \uc5bc\ub7ff \ub178\ucd9c");
            try {
                final WebDriverWait alertWait = new WebDriverWait(this.chromeDriver, Duration.ofSeconds(5L));
                final Alert alert = ((FluentWait<Object>)alertWait).until((Function<? super Object, Alert>)ExpectedConditions.alertIsPresent());
                System.out.println("\ud83d\udd14 Alert \uba54\uc2dc\uc9c0: " + alert.getText());
                alert.accept();
                System.out.println("\u2705 Alert \ud655\uc778 \ud074\ub9ad \uc644\ub8cc");
                System.out.println("\u2705 '\uc0ad\uc81c' \ubc84\ud2bc \ud074\ub9ad \uc644\ub8cc");
                Util.Sleep(2500, "\uc0ad\uc81c \ucc98\ub9ac\uae4c\uc9c0 \uc7a0\uc2dc \ub300\uae30\ud569\ub2c8\ub2e4.");
                this.chromeDriver.switchTo().defaultContent();
                return true;
            }
            catch (final TimeoutException te) {
                System.out.println("\u26a0\ufe0f Alert \ucc3d\uc774 \ub728\uc9c0 \uc54a\uc558\uc2b5\ub2c8\ub2e4 (\ubb34\uc2dc \uac00\ub2a5)");
                return false;
            }
        }
        catch (final Exception e) {
            System.err.println("\u274c \uc0ad\uc81c \ubc84\ud2bc \ud074\ub9ad \uc911 \uc624\ub958 \ubc1c\uc0dd: " + e.getMessage());
            return false;
        }
    }
}
