// 
// Decompiled by Procyon v0.6.0
// 

package chromeModule;

import org.openqa.selenium.Capabilities;
import java.io.InputStream;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chrome.ChromeDriver;

public class UndetectedChromeDriver extends ChromeDriver
{
    private boolean _headless;
    private Process _browser;
    private boolean _keepUserDataDir;
    private String _userDataDir;
    private ChromeOptions chromeOptions;
    
    @Override
    public void get(final String url) {
        if (this._headless) {
            this._headless();
        }
        this._cdcProps();
        super.get(url);
    }
    
    @Override
    public void quit() {
        super.quit();
        this._browser.destroyForcibly();
        if (this._keepUserDataDir) {
            for (int i = 0; i < 5; ++i) {
                try {
                    final File file = new File(this._userDataDir);
                    if (!file.exists()) {
                        break;
                    }
                    final boolean f = file.delete();
                    if (f) {
                        break;
                    }
                }
                catch (final Exception e) {
                    try {
                        Thread.sleep(300L);
                    }
                    catch (final Exception ex) {}
                }
            }
        }
    }
    
    public UndetectedChromeDriver(final ChromeOptions chromeOptions, final boolean headless, final boolean keepUserDataDir, final String userDataDir, final Process browser) {
        super(chromeOptions);
        this.chromeOptions = chromeOptions;
        this._browser = browser;
        this._headless = headless;
        this._keepUserDataDir = keepUserDataDir;
        this._userDataDir = userDataDir;
    }
    
    private void _headless() {
        final Object f = this.executeScript("return navigator.webdriver", new Object[0]);
        if (f == null) {
            return;
        }
        final Map<String, Object> params1 = new HashMap<String, Object>();
        params1.put("source", """
                              Object.defineProperty(window, 'navigator', {
                                  value: new Proxy(navigator, {
                                      has: (target, key) => (key === 'webdriver' ? false : key in target),
                                      get: (target, key) =>
                                          key === 'webdriver' ?
                                          false :
                                          typeof target[key] === 'function' ?
                                          target[key].bind(target) :
                                          target[key]
                                      })
                              });""");
        this.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", params1);
        final Map<String, Object> params2 = new HashMap<String, Object>();
        params2.put("userAgent", ((String)this.executeScript("return navigator.userAgent", new Object[0])).replace("Headless", ""));
        this.executeCdpCommand("Network.setUserAgentOverride", params2);
        final Map<String, Object> params3 = new HashMap<String, Object>();
        params3.put("source", "Object.defineProperty(navigator, 'maxTouchPoints', {get: () => 1});");
        this.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", params3);
        final Map<String, Object> params4 = new HashMap<String, Object>();
        params4.put("source", """
                              Object.defineProperty(navigator.connection, 'rtt', {get: () => 100});
                              // https://github.com/microlinkhq/browserless/blob/master/packages/goto/src/evasions/chrome-runtime.js
                              window.chrome = {
                                      app: {
                                          isInstalled: false,
                                          InstallState: {
                                              DISABLED: 'disabled',
                                              INSTALLED: 'installed',
                                              NOT_INSTALLED: 'not_installed'
                                          },
                                          RunningState: {
                                              CANNOT_RUN: 'cannot_run',
                                              READY_TO_RUN: 'ready_to_run',
                                              RUNNING: 'running'
                                          }
                                      },
                                      runtime: {
                                          OnInstalledReason: {
                                              CHROME_UPDATE: 'chrome_update',
                                              INSTALL: 'install',
                                              SHARED_MODULE_UPDATE: 'shared_module_update',
                                              UPDATE: 'update'
                                          },
                                          OnRestartRequiredReason: {
                                              APP_UPDATE: 'app_update',
                                              OS_UPDATE: 'os_update',
                                              PERIODIC: 'periodic'
                                          },
                                          PlatformArch: {
                                              ARM: 'arm',
                                              ARM64: 'arm64',
                                              MIPS: 'mips',
                                              MIPS64: 'mips64',
                                              X86_32: 'x86-32',
                                              X86_64: 'x86-64'
                                          },
                                          PlatformNaclArch: {
                                              ARM: 'arm',
                                              MIPS: 'mips',
                                              MIPS64: 'mips64',
                                              X86_32: 'x86-32',
                                              X86_64: 'x86-64'
                                          },
                                          PlatformOs: {
                                              ANDROID: 'android',
                                              CROS: 'cros',
                                              LINUX: 'linux',
                                              MAC: 'mac',
                                              OPENBSD: 'openbsd',
                                              WIN: 'win'
                                          },
                                          RequestUpdateCheckStatus: {
                                              NO_UPDATE: 'no_update',
                                              THROTTLED: 'throttled',
                                              UPDATE_AVAILABLE: 'update_available'
                                          }
                                      }
                              }
                              
                              // https://github.com/microlinkhq/browserless/blob/master/packages/goto/src/evasions/navigator-permissions.js
                              if (!window.Notification) {
                                      window.Notification = {
                                          permission: 'denied'
                                      }
                              }
                              
                              const originalQuery = window.navigator.permissions.query
                              window.navigator.permissions.__proto__.query = parameters =>
                                      parameters.name === 'notifications'
                                          ? Promise.resolve({ state: window.Notification.permission })
                                          : originalQuery(parameters)
                                      
                              const oldCall = Function.prototype.call 
                              function call() {
                                      return oldCall.apply(this, arguments)
                              }
                              Function.prototype.call = call
                              
                              const nativeToStringFunctionString = Error.toString().replace(/Error/g, 'toString')
                              const oldToString = Function.prototype.toString
                              
                              function functionToString() {
                                      if (this === window.navigator.permissions.query) {
                                          return 'function query() { [native code] }'
                                      }
                                      if (this === functionToString) {
                                          return nativeToStringFunctionString
                                      }
                                      return oldCall.call(oldToString, this)
                              }
                              // eslint-disable-next-line
                              Function.prototype.toString = functionToString""");
        this.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", params4);
    }
    
    private void _cdcProps() {
        final List<String> f = (List<String>)this.executeScript("""
                                                                let objectToInspect = window,
                                                                    result = [];
                                                                while(objectToInspect !== null)
                                                                { result = result.concat(Object.getOwnPropertyNames(objectToInspect));
                                                                  objectToInspect = Object.getPrototypeOf(objectToInspect); }
                                                                return result.filter(i => i.match(/.+_.+_(Array|Promise|Symbol)/ig))""", new Object[0]);
        if (f != null && f.size() > 0) {
            final Map<String, Object> param = new HashMap<String, Object>();
            param.put("source", """
                                let objectToInspect = window,
                                    result = [];
                                while(objectToInspect !== null)
                                { result = result.concat(Object.getOwnPropertyNames(objectToInspect));
                                  objectToInspect = Object.getPrototypeOf(objectToInspect); }
                                result.forEach(p => p.match(/.+_.+_(Array|Promise|Symbol)/ig)
                                                    &&delete window[p]&&console.log('removed',p))""");
            this.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", param);
        }
    }
    
    private void _stealth() {
        final StringBuilder stringBuffer = new StringBuilder();
        BufferedReader bufferedReader = null;
        try {
            final InputStream in = this.getClass().getResourceAsStream("/static/js/stealth.min.js");
            bufferedReader = new BufferedReader(new InputStreamReader(in));
            String str = null;
            while ((str = bufferedReader.readLine()) != null) {
                stringBuffer.append(str);
                stringBuffer.append("\n");
            }
            in.close();
            bufferedReader.close();
        }
        catch (final Exception ex) {}
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("source", stringBuffer.toString());
        this.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", params);
    }
    
    public void startSession(Capabilities capabilities) {
        if (capabilities == null) {
            capabilities = this.chromeOptions;
        }
        super.startSession(capabilities);
    }
}
