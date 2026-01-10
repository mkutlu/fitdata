export type UserProfileDto = {
    id: string;
    displayName: string;
    age: number;
    gender: string;
};

type FitbitProfileApiResponse = {
    user: {
        encodedId: string;
        fullName: string;
        age: number;
        gender: string; // "MALE"
    };
};

export async function fetchProfile(): Promise<UserProfileDto> {
    const res = await fetch("/api/profile");
    if (!res.ok) {
        const text = await res.text().catch(() => "");
        throw new Error(`Failed to load profile: ${res.status} ${text}`);
    }

    const data: FitbitProfileApiResponse = await res.json();

    return {
        id: data.user.encodedId,
        displayName: data.user.fullName,
        age: data.user.age,
        gender: data.user.gender.toLowerCase(),
    };
}
