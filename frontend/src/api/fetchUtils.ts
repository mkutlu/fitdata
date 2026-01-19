const API_BASE_URL = (window as any).ENV?.VITE_API_BASE_URL !== "__VITE_API_BASE_URL__" 
    ? (window as any).ENV?.VITE_API_BASE_URL 
    : (import.meta.env.VITE_API_BASE_URL || "");

if (API_BASE_URL) {
    console.log(`[DEBUG] API_BASE_URL is set to: "${API_BASE_URL}"`);
}

export async function fetchWithRetry(url: string, options: RequestInit = {}, retries = 2): Promise<Response> {
    const apiBase = API_BASE_URL.endsWith("/") ? API_BASE_URL.slice(0, -1) : API_BASE_URL;
    const cleanUrl = url.startsWith("/") ? url : `/${url}`;
    const fullUrl = url.startsWith("http") ? url : `${apiBase}${cleanUrl}`;
    
    // Safety check: prevent Mixed Content errors if possible
    if (window.location.protocol === "https:" && fullUrl.startsWith("http://")) {
        console.warn(`Mixed Content Warning: Requesting ${fullUrl} from an HTTPS page will likely be blocked.`);
    }

    // Safety check: common port error on Railway
    if (fullUrl.includes(".up.railway.app:8080")) {
        console.warn(`Connection Warning: You are requesting ${fullUrl}. Public Railway URLs usually don't use port 8080. If this times out, remove :8080 from VITE_API_BASE_URL.`);
    }

    const isStatusCheck = url.includes("/oauth/fitbit/status");
    const timeout = isStatusCheck ? 15000 : 30000; 
    
    const controller = new AbortController();
    const id = setTimeout(() => controller.abort(), timeout);
    
    const mergedOptions: RequestInit = {
        ...options,
        credentials: options.credentials || "include",
        signal: options.signal 
            ? anySignal([options.signal, controller.signal])
            : controller.signal
    };

    try {
        const res = await fetch(fullUrl, mergedOptions);
        clearTimeout(id);
        // Retry on server errors or common proxy timeout errors (408, 499, 502, 504)
        if (!res.ok && (res.status >= 500 || res.status === 408 || res.status === 499 || res.status === 502 || res.status === 504) && retries > 0) {
            console.warn(`Fetch failed with ${res.status} for ${fullUrl}, retrying... (${retries} left)`);
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
                console.warn(`Fetch timed out for ${fullUrl} after ${timeout}ms`);
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
            console.warn(`Fetch threw error for ${fullUrl}, retrying... (${retries} left)`, e);
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
