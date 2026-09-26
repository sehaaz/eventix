import { useEffect, useState } from 'react';
import client, { errorMessage } from '../api/client';
import EventCard from '../components/EventCard';

const PAGE_SIZE = 12;

export default function EventListPage() {
  const [form, setForm] = useState({ city: '', from: '', to: '' });
  const [filters, setFilters] = useState(form);
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    const params = { page, size: PAGE_SIZE };
    if (filters.city) params.city = filters.city;
    if (filters.from) params.from = new Date(`${filters.from}T00:00:00`).toISOString();
    if (filters.to) params.to = new Date(`${filters.to}T23:59:59`).toISOString();

    setError(null);
    client
      .get('/api/events', { params })
      .then((res) => setData(res.data))
      .catch((err) => setError(errorMessage(err)));
  }, [filters, page]);

  function handleSubmit(e) {
    e.preventDefault();
    setPage(0);
    setFilters(form);
  }

  function update(field) {
    return (e) => setForm({ ...form, [field]: e.target.value });
  }

  return (
    <div className="space-y-6">
      <form
        onSubmit={handleSubmit}
        className="flex flex-wrap items-end gap-3 rounded-xl border border-stone-200 bg-white p-4"
      >
        <label className="flex flex-col text-sm text-stone-500">
          Şehir
          <input
            value={form.city}
            onChange={update('city')}
            placeholder="Tümü"
            className="mt-1 rounded-md border border-stone-300 px-3 py-2 text-stone-800 focus:border-teal-500 focus:outline-none"
          />
        </label>
        <label className="flex flex-col text-sm text-stone-500">
          Başlangıç
          <input
            type="date"
            value={form.from}
            onChange={update('from')}
            className="mt-1 rounded-md border border-stone-300 px-3 py-2 text-stone-800 focus:border-teal-500 focus:outline-none"
          />
        </label>
        <label className="flex flex-col text-sm text-stone-500">
          Bitiş
          <input
            type="date"
            value={form.to}
            onChange={update('to')}
            className="mt-1 rounded-md border border-stone-300 px-3 py-2 text-stone-800 focus:border-teal-500 focus:outline-none"
          />
        </label>
        <button type="submit" className="rounded-md bg-teal-600 px-4 py-2 text-white hover:bg-teal-700">
          Filtrele
        </button>
      </form>

      {error && <p className="text-sm text-rose-600">{error}</p>}

      {data && data.content.length === 0 && (
        <p className="py-12 text-center text-stone-400">Etkinlik bulunamadı.</p>
      )}

      {data && data.content.length > 0 && (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {data.content.map((event) => (
              <EventCard key={event.id} event={event} />
            ))}
          </div>

          <div className="flex items-center justify-center gap-4 text-sm">
            <button
              onClick={() => setPage(page - 1)}
              disabled={page === 0}
              className="rounded-md border border-stone-300 px-3 py-1.5 hover:bg-stone-100 disabled:opacity-40"
            >
              Önceki
            </button>
            <span className="text-stone-500">
              {page + 1} / {data.page.totalPages}
            </span>
            <button
              onClick={() => setPage(page + 1)}
              disabled={page + 1 >= data.page.totalPages}
              className="rounded-md border border-stone-300 px-3 py-1.5 hover:bg-stone-100 disabled:opacity-40"
            >
              Sonraki
            </button>
          </div>
        </>
      )}
    </div>
  );
}
