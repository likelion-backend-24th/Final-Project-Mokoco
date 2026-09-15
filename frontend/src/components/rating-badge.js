"use client";

import { useEffect, useState } from "react";
import { Star } from "@phosphor-icons/react";

export default function RatingBadge({ email }) {
  const [data, setData] = useState(null);

  useEffect(() => {
    if (!email) return;
    const controller = new AbortController();
    fetch(`/api/reviews?revieweeEmail=${encodeURIComponent(email)}&size=1`, {
      signal: controller.signal,
      cache: "no-store",
    })
      .then((res) => (res.ok ? res.json() : null))
      .then((json) => {
        if (!controller.signal.aborted) setData(json);
      })
      .catch(() => {});
    return () => controller.abort();
  }, [email]);

  if (!data || data.averageRating == null) return null;

  return (
    <span className="inline-flex items-center gap-0.5 text-xs font-semibold text-amber-500">
      <Star size={13} weight="fill" />
      {data.averageRating.toFixed(1)}
      <span className="text-slate-400">({data.totalCount})</span>
    </span>
  );
}
