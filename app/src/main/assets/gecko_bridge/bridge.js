(function () {
  if (window.SafeScanAndroid) return;
  const call = (method, value) => {
    const message = {method: method};
    if (value !== undefined) message.value = value;
    // runtime.sendMessage is delivered to GeckoView's native MessageDelegate.
    return browser.runtime.sendNativeMessage("safescan", message).then(result => {
      if (method === "startScan" && result) {
        try { window.receiveScan(typeof result === "string" ? JSON.parse(result) : result); } catch (e) { console.error(e); }
      }
      return result;
    });
  };
  window.SafeScanAndroid = {
    setLocale: value => call("setLocale", value),
    startScan: value => call("startScan", value),
    exportPdf: value => call("exportPdf", value),
    emailPdf: value => call("emailPdf", value),
    backup: value => call("backup", value),
    restoreBackup: () => call("restoreBackup"),
    repairPlan: () => call("repairPlan"),
    shareApp: () => call("shareApp"),
    updateApp: () => call("updateApp"),
    openSettings: value => call("openSettings", value)
  };
})();
