export async function fetchWithRetry(url: string, options: RequestInit = {}, retries = 2): Promise<Response> {
    const isStatusCheck = url.includes("/oauth/fitbit/status");
    const timeout = isStatusCheck ? 10000 : 20000; // Reset to reasonable 10s and 20s
    
    const controller = new AbortController();
    const id = setTimeout(() => controller.abort(), timeout);
    
    const mergedOptions = {
        ...options,
        signal: options.signal 
            ? anySignal([options.signal, controller.signal])
            : controller.signal
    };

    try {
        const res = await fetch(url, mergedOptions);
        clearTimeout(id);
        // Retry on server errors or common proxy timeout errors (408, 499, 502, 504)
        if (!res.ok && (res.status >= 500 || res.status === 401 || res.status === 408 || res.status === 499 || res.status === 502 || res.status === 504) && retries > 0) {
            console.warn(`Fetch failed with ${res.status} for ${url}, retrying... (${retries} left)`);
            // Exponential backoff
            const delay = (3 - retries) * 2000;
            await new Promise(resolve => setTimeout(resolve, delay));
            return fetchWithRetry(url, options, retries - 1);
        }
        return res;
    } catch (e) {
        clearTimeout(id);
        if (e instanceof Error && e.name === "AbortError") {
            // If it was our own timeout that triggered it
            if (controller.signal.aborted && (!options.signal || !options.signal.aborted)) {
                console.warn(`Fetch timed out for ${url} after ${timeout}ms`);
                if (retries > 0) {
                    console.warn(`Retrying after timeout... (${retries} left)`);
                    const delay = (3 - retries) * 2000;
                    await new Promise(resolve => setTimeout(resolve, delay));
                    return fetchWithRetry(url, options, retries - 1);
                }
            }
            throw e;
        }

        if (retries > 0) {
            console.warn(`Fetch threw error for ${url}, retrying... (${retries} left)`, e);
            const delay = (3 - retries) * 2000;
            await new Promise(resolve => setTimeout(resolve, delay));
            return fetchWithRetry(url, options, retries - 1);
        }
        throw e;
    }
}

/**
 * Combines multiple AbortSignals into one.
 * Available in modern browsers as AbortSignal.any(), but for compatibility:
 */
function anySignal(signals: AbortSignal[]): AbortSignal {
    if ((AbortSignal as any).any) return (AbortSignal as any).any(signals);
    
    const controller = new AbortController();
    for (const signal of signals) {
        if (signal.aborted) {
            controller.abort();
            return signal;
        }
        signal.addEventListener("abort", () => controller.abort(), { once: true });
    }
    return controller.signal;
}
