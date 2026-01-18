import { useEffect, useState } from "react";
import { fetchReadiness, type ReadinessCardDto } from "../api/readinessApi";

type Props = {
    baseDate: string;
};

export function ReadinessCard({ baseDate }: Props) {
    const [data, setData] = useState<ReadinessCardDto | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError(null);

        (async () => {
            try {
                const d = await fetchReadiness(baseDate, controller.signal);
                setData(d);
            } catch (e) {
                if (e instanceof Error && e.name === "AbortError") return;
                setError(e instanceof Error ? e.message : "Unknown error");
            } finally {
                setLoading(false);
            }
        })();

        return () => { controller.abort(); };
    }, [baseDate]);

    if (loading) return (
        <div className="bg-gray-900/40 border border-gray-800 p-6 rounded-2xl shadow-xl animate-pulse h-[280px] flex items-center justify-center">
            <div className="w-48 h-48 rounded-full border-8 border-gray-800"></div>
        </div>
    );
    
    if (error) return (
        <div className="bg-red-900/10 border border-red-900/30 text-red-400 p-6 rounded-2xl h-[280px] flex flex-col items-center justify-center">
            <span className="text-lg font-semibold mb-2">Failed to load data</span>
            <p className="text-sm opacity-80 text-center">{error}</p>
        </div>
    );
    
    if (!data) return null;

    const {
        readinessScore,
        readinessStatus,
        vo2Max,
        exerciseDays
    } = data;

    const rScore = readinessScore ?? 0;
    const exDays = exerciseDays ?? 0;

    // SVG Geometry
    const size = 260;
    const strokeWidth = 14;
    const radius = (size - strokeWidth * 3) / 2;
    const center = size / 2;
    
    // Circumference and segment lengths
    const semiCircumference = Math.PI * radius;
    const gap = 8; // degrees
    const gapRad = (gap * Math.PI) / 180;
    
    // Readiness (Top)
    const rStartAngle = -Math.PI + gapRad/2;
    const rEndAngle = -gapRad/2;
    const rArcLength = semiCircumference - (gapRad * radius);
    const rOffset = rArcLength - (Math.min(rScore, 100) / 100) * rArcLength;

    // Exercise Days (Bottom)
    const cStartAngle = gapRad/2;
    const cEndAngle = Math.PI - gapRad/2;
    const cArcLength = semiCircumference - (gapRad * radius);
    
    // Scale for Exercise Days (0 to 7)
    const cOffset = cArcLength - (Math.min(exDays, 7) / 7) * cArcLength;

    const describeArc = (x: number, y: number, r: number, startAngle: number, endAngle: number, reverse: boolean = false) => {
        const start = {
            x: x + r * Math.cos(reverse ? startAngle : endAngle),
            y: y + r * Math.sin(reverse ? startAngle : endAngle)
        };
        const end = {
            x: x + r * Math.cos(reverse ? endAngle : startAngle),
            y: y + r * Math.sin(reverse ? endAngle : startAngle)
        };
        const sweepFlag = reverse ? "1" : "0";
        const largeArcFlag = Math.abs(endAngle - startAngle) <= Math.PI ? "0" : "1";
        return [
            "M", start.x, start.y,
            "A", r, r, 0, largeArcFlag, sweepFlag, end.x, end.y
        ].join(" ");
    };

    return (
        <div className="bg-gray-900/40 border border-gray-800 p-6 rounded-2xl shadow-xl flex items-center justify-center relative overflow-hidden group h-full w-full">
            <div className="relative w-full h-full max-w-full max-h-full aspect-square flex items-center justify-center">
                <svg 
                    viewBox={`0 0 ${size} ${size}`}
                    className="w-full h-full max-w-[400px] max-h-[400px] transition-all duration-300"
                >
                    {/* Readiness Background (Top) */}
                    <path
                        d={describeArc(center, center, radius, rStartAngle, rEndAngle, true)}
                        fill="none"
                        stroke="#1f2937"
                        strokeWidth={strokeWidth}
                        strokeLinecap="round"
                    />
                    {/* Readiness Progress (Top) */}
                    <path
                        d={describeArc(center, center, radius, rStartAngle, rEndAngle, true)}
                        fill="none"
                        stroke="#10b981"
                        strokeWidth={strokeWidth}
                        strokeDasharray={rArcLength}
                        strokeDashoffset={rOffset}
                        strokeLinecap="round"
                        className="transition-all duration-1000 ease-out"
                    />

                    {/* Exercise Days Background (Bottom) */}
                    <path
                        d={describeArc(center, center, radius, cStartAngle, cEndAngle)}
                        fill="none"
                        stroke="#1f2937"
                        strokeWidth={strokeWidth}
                        strokeLinecap="round"
                    />
                    
                    {/* Exercise Days Progress (Bottom) */}
                    <path
                        d={describeArc(center, center, radius, cStartAngle, cEndAngle)}
                        fill="none"
                        stroke="#3b82f6"
                        strokeWidth={strokeWidth}
                        strokeDasharray={cArcLength}
                        strokeDashoffset={cOffset}
                        strokeLinecap="round"
                        className="transition-all duration-1000 ease-out"
                    />
                </svg>

                {/* Center Content */}
                <div className="absolute inset-0 flex flex-col items-center justify-center text-center p-4">
                    <div className="flex flex-col items-center scale-[0.8] sm:scale-100 transition-transform">
                        <div className="mb-2">
                            <span className="block text-[10px] sm:text-xs text-gray-400 uppercase tracking-widest font-semibold mb-1">Readiness</span>
                            <div className="flex items-center justify-center">
                                <span className="text-2xl sm:text-4xl font-black text-white leading-none">{rScore}</span>
                            </div>
                            <span className="block text-[8px] sm:text-[10px] text-emerald-400 font-medium mt-1">{readinessStatus || "N/A"}</span>
                        </div>
                        
                        <div className="w-12 sm:w-16 h-[1px] bg-gray-800 my-1"></div>
                        
                        <div className="mt-2">
                            <div className="flex flex-col items-center">
                                <span className="text-xl sm:text-3xl font-black text-white leading-none">{exDays}/7</span>
                                {vo2Max && (
                                    <span className="text-[8px] sm:text-[10px] text-purple-400 font-bold mt-1 bg-purple-900/20 px-2 py-0.5 rounded-full border border-purple-900/30">
                                        VO2: {vo2Max}
                                    </span>
                                )}
                            </div>
                            <span className="block text-[10px] sm:text-xs text-gray-400 uppercase tracking-widest font-semibold mt-1">Exercise Days</span>
                            <span className="text-[8px] sm:text-[10px] text-gray-500 mt-1 block italic">
                                Current week
                            </span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
