const STEPS = ['Sipariş alındı', 'Kontenjan ayrıldı', 'Biletleriniz hazır'];

// Her durumda kaç adımın tamamlandığı.
const DONE_COUNT = { PENDING: 1, QUOTA_RESERVED: 2, COMPLETED: 3 };

// FAILED'da hangi adımın başarısız olduğu failureReason'dan çıkar.
const FAILED_STEP = { QUOTA: 1, TICKET: 2 };

const FAILURE_MESSAGES = {
  QUOTA: 'Yeterli kontenjan kalmadı.',
  TICKET: 'Biletler üretilemedi, ayrılan kontenjan iade edildi.',
};

function stepState(index, status, failureReason) {
  if (status === 'FAILED') {
    const failedStep = FAILED_STEP[failureReason];
    if (index < failedStep) return 'done';
    return index === failedStep ? 'failed' : 'idle';
  }
  const done = DONE_COUNT[status];
  if (index < done) return 'done';
  return index === done ? 'active' : 'idle';
}

function StepIcon({ state }) {
  if (state === 'done') {
    return (
      <span className="flex h-8 w-8 items-center justify-center rounded-full bg-emerald-500 text-white">
        ✓
      </span>
    );
  }
  if (state === 'failed') {
    return (
      <span className="flex h-8 w-8 items-center justify-center rounded-full bg-rose-500 text-white">
        ✕
      </span>
    );
  }
  if (state === 'active') {
    return (
      <span className="flex h-8 w-8 items-center justify-center rounded-full border-2 border-teal-200">
        <span className="h-4 w-4 animate-spin rounded-full border-2 border-teal-600 border-t-transparent" />
      </span>
    );
  }
  return <span className="h-8 w-8 rounded-full border-2 border-stone-200" />;
}

const LABEL_COLORS = {
  done: 'text-stone-900',
  active: 'text-teal-700',
  failed: 'text-rose-600',
  idle: 'text-stone-400',
};

export default function SagaStepper({ status, failureReason }) {
  return (
    <div className="space-y-6">
      <ol className="space-y-4">
        {STEPS.map((label, index) => {
          const state = stepState(index, status, failureReason);
          return (
            <li key={label} className="flex items-center gap-3">
              <StepIcon state={state} />
              <span className={`font-medium ${LABEL_COLORS[state]}`}>{label}</span>
            </li>
          );
        })}
      </ol>
      {status === 'FAILED' && (
        <p className="rounded-md bg-rose-50 p-3 text-sm text-rose-700">
          {FAILURE_MESSAGES[failureReason] ?? failureReason}
        </p>
      )}
    </div>
  );
}
