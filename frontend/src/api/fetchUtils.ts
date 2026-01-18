export async function fetchWithRetry(url: string, options: RequestInit = {}, retries = 2): Promise<Response> {
    try {
        const res = await fetch(url, options);
        // Retry on server errors or common proxy timeout errors (408, 499, 502, 504)
        if (!res.ok && (res.status >= 500 || res.status === 408 || res.status === 499 || res.status === 502 || res.status === 504) && retries > 0) {
            console.warn(`Fetch failed with ${res.status} for ${url}, retrying... (${retries} left)`);
            // Exponential backoff
            const delay = (3 - retries) * 1500;
            await new Promise(resolve => setTimeout(resolve, delay));
            return fetchWithRetry(url, options, retries - 1);
        }
        return res;
    } catch (e) {
        if (e instanceof Error && e.name === "AbortError") throw e;

        if (retries > 0) {
            console.warn(`Fetch threw error for ${url}, retrying... (${retries} left)`, e);
            const delay = (3 - retries) * 1500;
            await new Promise(resolve => setTimeout(resolve, delay));
            return fetchWithRetry(url, options, retries - 1);
        }
        throw e;
    }
}
