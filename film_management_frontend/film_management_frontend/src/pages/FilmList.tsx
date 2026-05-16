import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Movie } from '../types/index';
import { Clock, Loader2, AlertCircle } from 'lucide-react';
import { movieService } from '../services/movie.service';
import { useCache } from '../context/CacheContext';

const FilmList: React.FC = () => {
  const { filmListCache, setFilmListCache } = useCache();
  const [movies, setMovies] = useState<Movie[]>(filmListCache?.movies || []);
  const [isLoading, setIsLoading] = useState(!filmListCache);
  const [isFetchingMore, setIsFetchingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(filmListCache?.page || 0);
  const [hasMore, setHasMore] = useState(filmListCache ? filmListCache.hasMore : true);
  const navigate = useNavigate();

  const observer = useRef<IntersectionObserver | null>(null);
  const lastElementRef = useCallback((node: HTMLDivElement | null) => {
    if (isLoading || isFetchingMore) return;
    if (observer.current) observer.current.disconnect();

    observer.current = new IntersectionObserver(entries => {
      if (entries[0].isIntersecting && hasMore) {
        setPage(prevPage => prevPage + 1);
      }
    }, {
      rootMargin: '100px',
      threshold: 0.1
    });

    if (node) observer.current.observe(node);
  }, [isLoading, isFetchingMore, hasMore]);

  useEffect(() => {
    const fetchFilms = async () => {
      // Nếu đã có cache và đang ở page của cache thì không fetch lại
      if (filmListCache && page <= filmListCache.page && movies.length > 0) {
        setIsLoading(false);
        return;
      }

      try {
        if (page === 0) {
          setIsLoading(true);
        } else {
          setIsFetchingMore(true);
        }

        const pageResponse = await movieService.getPageMovies(page, 10);
        const newHasMore = page < pageResponse.totalPages - 1;

        let updatedMovies: Movie[];
        if (page === 0) {
          updatedMovies = pageResponse.data;
        } else {
          updatedMovies = [...movies, ...pageResponse.data];
        }

        setMovies(updatedMovies);
        setHasMore(newHasMore);
        
        // Cập nhật cache
        setFilmListCache({
          movies: updatedMovies,
          page: page,
          hasMore: newHasMore
        });
      } catch (err: any) {
        setError('Không thể tải danh sách phim. Vui lòng thử lại sau.');
      } finally {
        setIsLoading(false);
        setIsFetchingMore(false);
      }
    };

    fetchFilms();
  }, [page]);

  if (isLoading && page === 0) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[50vh] space-y-4">
        <Loader2 className="w-12 h-12 text-green-500 animate-spin" />
        <p className="text-neutral-400 animate-pulse">Đang tải danh sách phim...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[50vh] space-y-4">
        <div className="p-4 bg-red-500/10 border border-red-500/20 rounded-xl text-red-500 flex items-center gap-3">
          <AlertCircle className="w-6 h-6" />
          <p>{error}</p>
        </div>
        <button 
          onClick={() => window.location.reload()}
          className="px-6 py-2 bg-neutral-800 hover:bg-neutral-700 text-white rounded-lg transition-colors"
        >
          Thử lại
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div className="flex justify-between items-end">
        <div>
          <h1 className="text-4xl font-bold text-white">Phim đang chiếu</h1>
          <p className="text-neutral-400 mt-2">Danh sách những bộ phim tại rạp</p>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
        {movies.map((movie, index) => (
          <div
            key={movie.idMovie}
            ref={index === movies.length - 1 ? lastElementRef : null}
            className="group relative bg-neutral-900 rounded-xl overflow-hidden border border-neutral-800 hover:border-green-500/50 transition-all hover:scale-[1.02] cursor-pointer shadow-lg"
            onClick={() => navigate(`/movie/${movie.idMovie}`)}
          >
            <div className="aspect-[2/3] relative overflow-hidden">
              <img
                src={movie.image}
                className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-neutral-950 via-transparent to-transparent opacity-60" />
            </div>

            <div className="p-4 space-y-3">
              <h3 className="text-lg font-bold text-white line-clamp-1 group-hover:text-green-500 transition-colors">
                {movie.nameMovie}
              </h3>
              
              <div className="flex flex-wrap gap-2">
                {movie.categories?.map((c) => (
                  <span
                    key={c.idCategory}
                    className="text-[10px] uppercase tracking-wider font-bold px-2 py-0.5 bg-neutral-800 rounded-full text-neutral-400 border border-neutral-700"
                  >
                    {c.idCategory}
                  </span>
                ))}
              </div>

              <div className="flex items-center justify-between text-xs text-neutral-400 pt-2 border-t border-neutral-800">
                <div className="flex items-center gap-1">
                  <Clock className="w-3 h-3" />
                  {movie.duration} phút
                </div>
              </div>

              <button className="w-full py-2 bg-green-600 hover:bg-green-700 text-white text-sm font-bold rounded-lg transition-colors mt-2 opacity-0 group-hover:opacity-100 transform translate-y-2 group-hover:translate-y-0 transition-all duration-300">
                Đặt vé ngay
              </button>
            </div>
          </div>
        ))}
      </div>

      {isFetchingMore && (
        <div className="flex justify-center py-8">
          <Loader2 className="w-8 h-8 text-green-500 animate-spin" />
        </div>
      )}

      {!hasMore && movies.length > 0 && (
        <div className="text-center py-8 text-neutral-500 italic">
          Bạn đã xem hết danh sách phim.
        </div>
      )}
    </div>
  );
};

export default FilmList;
