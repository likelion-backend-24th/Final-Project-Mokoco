export default function Home() {
  return (
    <main className="flex min-h-screen items-center justify-center px-6 py-16">
      <section className="w-full max-w-2xl rounded-3xl border border-emerald-100 bg-white p-8 shadow-xl shadow-emerald-100/60 sm:p-12">
        <p className="mb-3 text-sm font-semibold tracking-[0.2em] text-emerald-600">
          MOKOCO
        </p>
        <h1 className="text-4xl font-bold tracking-tight text-slate-900 sm:text-5xl">
          가까운 이웃과 함께해요.
        </h1>
        <p className="mt-5 max-w-xl text-lg leading-8 text-slate-600">
          Mokoco 프론트엔드 개발 환경이 준비되었습니다. 이제 지역 검색과
          커뮤니티 기능을 연결할 수 있습니다.
        </p>

        <div className="mt-10 grid gap-4 sm:grid-cols-3">
          {[
            ["Next.js", "16 / App Router"],
            ["Language", "JavaScript"],
            ["Style", "Tailwind CSS 4"],
          ].map(([label, value]) => (
            <div key={label} className="rounded-2xl bg-slate-50 p-4">
              <p className="text-sm text-slate-500">{label}</p>
              <p className="mt-1 font-semibold text-slate-900">{value}</p>
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}
