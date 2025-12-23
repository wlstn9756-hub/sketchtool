// 
// Decompiled by Procyon v0.6.0
// 

package com.devco;

import java.util.concurrent.Callable;
import org.openqa.selenium.WebElement;
import util.Util;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeDriver;

public class GoogleAuthSetting
{
    private final ChromeDriver chromeDriver;
    private final JavascriptExecutor jsExecutor;
    
    public GoogleAuthSetting(final ChromeDriver chromeDriver) {
        this.chromeDriver = chromeDriver;
        this.jsExecutor = chromeDriver;
    }
    
    public void disableClipboardDirectV5Fix() {
        try {
            this.chromeDriver.get("chrome://settings/content/clipboard");
            Util.Sleep(800, "\ud074\ub9bd\ubcf4\ub4dc \uc124\uc815 \ud398\uc774\uc9c0 \ub85c\ub4dc");
            this.waitDeep(() -> this.jsExecutor.executeScript(this.deepQueryDef() + "return deepQuery('settings-category-default-radio-group, settings-three-state-default-radio-group');", new Object[0]), 5000L);
            final WebElement disabled = this.waitDeep(() -> this.jsExecutor.executeScript(this.deepQueryDef() + "const grp = deepQuery('#settingsCategoryDefaultRadioGroup');if (grp && grp.shadowRoot) {  const gsr = grp.shadowRoot;  const hit = gsr.querySelector('settings-collapse-radio-button#disabledRadioOption,                                    settings-collapse-radio-button[name=\"0\"]');  if (hit) return hit;}return deepQuery('settings-collapse-radio-button#disabledRadioOption,                   settings-collapse-radio-button[name=\"0\"]');", new Object[0]), 8000L);
            if (disabled == null) {
                throw new RuntimeException("#disabledRadioOption not found");
            }
            this.jsExecutor.executeScript("arguments[0].scrollIntoView({block:'center'});", disabled);
            Util.Sleep(150, "OFF \ub77c\ub514\uc624 \uc2a4\ud06c\ub864");
            this.jsExecutor.executeScript("const host=arguments[0]; const sr=host?.shadowRoot;const rb = sr?.querySelector('cr-radio-button');const rbs= rb?.shadowRoot;const tgt= (rbs?.querySelector('#button, #disc, #label, button, input[type=radio]')) || rb || host;tgt.click();", disabled);
            Util.Sleep(250, "OFF \ud074\ub9ad 1\ucc28 \uc2dc\ub3c4");
            boolean ok = this.waitTrue(() -> this.jsExecutor.executeScript(this.deepQueryDef() + "const dis = deepQuery('settings-collapse-radio-button#disabledRadioOption,                       settings-collapse-radio-button[name=\"0\"]');const grp = deepQuery('#settingsCategoryDefaultRadioGroup');const dsr = dis?.shadowRoot;const rb  = dsr?.querySelector('cr-radio-button');const rbs = rb?.shadowRoot;const input = rbs?.querySelector('input[type=radio]');const byProp  = !!rb?.checked; const byAttr  = !!rb?.hasAttribute?.('checked'); const byAria  = (rb?.getAttribute?.('aria-checked') === 'true'); const byInput = !!(input?.checked); const byGroup = !!(grp && dis && (grp.selected === dis.getAttribute('name') || grp.selected === '0')); return (byProp || byAttr || byAria || byInput || byGroup);", new Object[0]), 4000L);
            if (!ok) {
                this.jsExecutor.executeScript(this.deepQueryDef() + "const grp = deepQuery('#settingsCategoryDefaultRadioGroup');const dis = deepQuery('settings-collapse-radio-button#disabledRadioOption,                       settings-collapse-radio-button[name=\"0\"]');try { if (grp && dis) {   grp.selected = dis.getAttribute('name') || '0';   grp.dispatchEvent(new CustomEvent('iron-select', {bubbles:true, composed:true})); } } catch(e) {}", new Object[0]);
                Util.Sleep(250, "\uadf8\ub8f9 selected=0 \uac15\uc81c");
                ok = this.waitTrue(() -> this.jsExecutor.executeScript(this.deepQueryDef() + "const dis = deepQuery('settings-collapse-radio-button#disabledRadioOption,                       settings-collapse-radio-button[name=\"0\"]');const grp = deepQuery('#settingsCategoryDefaultRadioGroup');const dsr = dis?.shadowRoot;const rb  = dsr?.querySelector('cr-radio-button');const rbs = rb?.shadowRoot;const input = rbs?.querySelector('input[type=radio]');const byProp  = !!rb?.checked; const byAttr  = !!rb?.hasAttribute?.('checked'); const byAria  = (rb?.getAttribute?.('aria-checked') === 'true'); const byInput = !!(input?.checked); const byGroup = !!(grp && dis && (grp.selected === dis.getAttribute('name') || grp.selected === '0')); return (byProp || byAttr || byAria || byInput || byGroup);", new Object[0]), 2500L);
            }
            if (!ok) {
                throw new RuntimeException("OFF \ub77c\ub514\uc624 checked \ud655\uc778 \uc2e4\ud328");
            }
            Util.Sleep(250, "\ud074\ub9bd\ubcf4\ub4dc \uad8c\ud55c: \ucc28\ub2e8(OFF) \uc644\ub8cc");
        }
        catch (final Exception e) {
            e.printStackTrace();
            throw new RuntimeException("disableClipboardDirectV5Fix() \ucc98\ub9ac \uc911 \uc624\ub958", e);
        }
    }
    
    private String deepQueryDef() {
        return "const deepQuery=(sel)=>{  const seen=new Set(); const q=[document];  while(q.length){    const root=q.shift();    try{ const el=root.querySelector(sel); if(el) return el; }catch(e){}    const all=root.querySelectorAll ? root.querySelectorAll('*') : [];     for(const n of all){ const sr=n.shadowRoot; if(sr && !seen.has(sr)){ seen.add(sr); q.push(sr);} }  } return null;};";
    }
    
    private WebElement waitDeep(final Callable<WebElement> finder, final long timeoutMs) throws Exception {
        final long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            try {
                final WebElement e = finder.call();
                if (e != null) {
                    return e;
                }
            }
            catch (final Exception ex) {}
            Thread.sleep(150L);
        }
        return null;
    }
    
    private boolean waitTrue(final Callable<Boolean> cond, final long timeoutMs) throws Exception {
        final long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            try {
                final Boolean r = cond.call();
                if (r != null && r) {
                    return true;
                }
            }
            catch (final Exception ex) {}
            Thread.sleep(120L);
        }
        return false;
    }
}
