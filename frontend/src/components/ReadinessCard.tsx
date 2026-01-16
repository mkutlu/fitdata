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
        let alive = true;
        setLoading(true);
        setError(null);

        (async () => {
            try {
                const d = await fetchReadiness(baseDate);
                if (alive) setData(d);
            } catch (e) {
                if (alive) setError(e instanceof Error ? e.message : "Unknown error");
            } finally {
                if (alive) setLoading(false);
            }
        })();

        return () => { alive = false; };
    }, [baseDate]);

    if (loading) return (
        <div className="bg-gray-900/40 border border-gray-800 p-6 rounded-2xl shadow-xl animate-pulse h-[280px] flex items-center justify-center">
            <div className="w-48 h-48 rounded-full border-8 border-gray-800"></div>
        </div>
    );
    
    if (error) return (
        <div className="bg-red-900/10 border border-red-900/30 text-red-400 p-6 rounded-2xl h-[280px] flex flex-col items-center justify-center">
            <span className="text-lg font-semibold mb-2">Veri Yüklenemedi</span>
            <p className="text-sm opacity-80 text-center">{error}</p>
        </div>
    );
    
    if (!data) return null;

    const {
        readinessScore,
        cardioLoadScore,
        cardioLoadTargetMin,
        cardioLoadTargetMax,
        readinessStatus,
        cardioLoadStatus,
        vo2Max
    } = data;

    const rScore = readinessScore ?? 0;
    const cScore = cardioLoadScore ?? 0;
    const tMin = cardioLoadTargetMin ?? 0;
    const tMax = cardioLoadTargetMax ?? 100;

    // SVG Geometry
    const size = 260;
    const strokeWidth = 14;
    const radius = (size - strokeWidth * 3) / 2;
    const center = size / 2;
    
    // Circumference and segment lengths
    // We want two semi-circles with a small gap
    const semiCircumference = Math.PI * radius;
    const gap = 8; // degrees
    const gapRad = (gap * Math.PI) / 180;
    
    // Readiness (Top)
    const rStartAngle = -Math.PI + gapRad/2;
    const rEndAngle = -gapRad/2;
    const rArcLength = semiCircumference - (gapRad * radius);
    const rOffset = rArcLength - (Math.min(rScore, 100) / 100) * rArcLength;

    // Cardio Load (Bottom)
    const cStartAngle = gapRad/2;
    const cEndAngle = Math.PI - gapRad/2;
    const cArcLength = semiCircumference - (gapRad * radius);
    
    // Scale for Cardio Load
    const cMaxScale = Math.max(cScore, tMax, 10) * 1.1;
    const cOffset = cArcLength - (Math.min(cScore, cMaxScale) / cMaxScale) * cArcLength;

    // Target range for Cardio Load
    const targetStartOffset = cArcLength - (tMin / cMaxScale) * cArcLength;
    const targetEndOffset = cArcLength - (tMax / cMaxScale) * cArcLength;

    const describeArc = (x: number, y: number, r: number, startAngle: number, endAngle: number) => {
        const start = {
            x: x + r * Math.cos(endAngle),
            y: y + r * Math.sin(endAngle)
        };
        const end = {
            x: x + r * Math.cos(startAngle),
            y: y + r * Math.sin(startAngle)
        };
        const largeArcFlag = endAngle - startAngle <= Math.PI ? "0" : "1";
        return [
            "M", start.x, start.y,
            "A", r, r, 0, largeArcFlag, 0, end.x, end.y
        ].join(" ");
    };

    return (
        <div className="bg-gray-900/40 border border-gray-800 p-6 rounded-2xl shadow-xl flex items-center justify-center relative overflow-hidden group">
            <div className="relative" style={{ width: size, height: size }}>
                <svg width={size} height={size} className="transform rotate-0">
                    {/* Readiness Background (Top) */}
                    <path
                        d={describeArc(center, center, radius, rStartAngle, rEndAngle)}
                        fill="none"
                        stroke="#1f2937"
                        strokeWidth={strokeWidth}
                        strokeLinecap="round"
                    />
                    {/* Readiness Progress (Top) */}
                    <path
                        d={describeArc(center, center, radius, rStartAngle, rEndAngle)}
                        fill="none"
                        stroke="#10b981"
                        strokeWidth={strokeWidth}
                        strokeDasharray={rArcLength}
                        strokeDashoffset={rOffset}
                        strokeLinecap="round"
                        className="transition-all duration-1000 ease-out"
                    />

                    {/* Cardio Load Background (Bottom) */}
                    <path
                        d={describeArc(center, center, radius, cStartAngle, cEndAngle)}
                        fill="none"
                        stroke="#1f2937"
                        strokeWidth={strokeWidth}
                        strokeLinecap="round"
                    />
                    
                    {/* Cardio Load Target Range */}
                    <path
                        d={describeArc(center, center, radius, cStartAngle, cEndAngle)}
                        fill="none"
                        stroke="#3b82f6"
                        strokeWidth={strokeWidth + 4}
                        strokeDasharray={`${targetStartOffset - targetEndOffset} ${cArcLength}`}
                        strokeDashoffset={targetStartOffset}
                        className="opacity-20"
                    />

                    {/* Cardio Load Progress (Bottom) */}
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
                    
                    {/* Target Markers */}
                    <path
                        d={describeArc(center, center, radius, cStartAngle, cEndAngle)}
                        fill="none"
                        stroke="white"
                        strokeWidth={strokeWidth + 4}
                        strokeDasharray={`2 ${cArcLength}`}
                        strokeDashoffset={targetStartOffset}
                        className="opacity-50"
                    />
                    <path
                        d={describeArc(center, center, radius, cStartAngle, cEndAngle)}
                        fill="none"
                        stroke="white"
                        strokeWidth={strokeWidth + 4}
                        strokeDasharray={`2 ${cArcLength}`}
                        strokeDashoffset={targetEndOffset}
                        className="opacity-50"
                    />
                </svg>

                {/* Center Content */}
                <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
                    <div className="mb-4">
                        <span className="block text-xs text-gray-400 uppercase tracking-widest font-semibold mb-1">Readiness</span>
                        <div className="flex items-center justify-center">
                            <span className="text-4xl font-black text-white leading-none">{rScore}</span>
                        </div>
                        <span className="block text-[10px] text-emerald-400 font-medium mt-1">{readinessStatus || "N/A"}</span>
                    </div>
                    
                    <div className="w-16 h-[1px] bg-gray-800 my-1"></div>
                    
                    <div className="mt-4">
                        <span className="block text-[10px] text-blue-400 font-medium mb-1">{cardioLoadStatus || "N/A"}</span>
                        <div className="flex flex-col items-center">
                            <span className="text-3xl font-black text-white leading-none">{cScore}</span>
                            {vo2Max && (
                                <span className="text-[10px] text-purple-400 font-bold mt-1 bg-purple-900/20 px-2 py-0.5 rounded-full border border-purple-900/30">
                                    VO2: {vo2Max}
                                </span>
                            )}
                        </div>
                        <span className="block text-xs text-gray-400 uppercase tracking-widest font-semibold mt-1">Cardio Load</span>
                        <span className="text-[10px] text-gray-500 mt-2 block">
                            Target: <span className="text-gray-300 font-mono">{tMin}-{tMax}</span>
                        </span>
                    </div>
                </div>
            </div>
        </div>
    );
}
