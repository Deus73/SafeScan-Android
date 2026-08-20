// Keep the background page alive for GeckoView's native WebExtension delegate.
browser.runtime.onMessage.addListener(message => Promise.resolve(message));
