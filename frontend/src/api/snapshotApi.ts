import { fetchWithRetry } from "./fetchUtils";

export async function fetchSnapshot(id: string, signal?: AbortSignal) {
    const res = await fetchWithRetry(`/api/snapshots/${id}`, { signal }, 0);
    if (!res.ok) {
        throw new Error("Failed to fetch snapshot");
    }
    return res.json();
}

export async function createSnapshot(params: { selectedDate: string, stepsRange: string, weightRange: string }) {
    const { selectedDate, stepsRange, weightRange } = params;
    const url = `/api/snapshots?selectedDate=${selectedDate}&stepsRange=${stepsRange}&weightRange=${weightRange}`;
    const res = await fetchWithRetry(url, { method: "POST" }, 0);
    if (!res.ok) {
        throw new Error("Failed to create snapshot");
    }
    return res.text(); // Returns UUID as plain string
}
