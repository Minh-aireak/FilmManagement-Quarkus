import React, { useEffect, useState, useRef, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Plus, Edit, Trash2, Search, Loader2, Film as FilmIcon } from 'lucide-react';
import { movieService } from '../services/movie.service';
import { type Movie } from '../types';
import { useToast } from '../components/Toast';
import { useCache } from '../context/CacheContext';

const AdminFilms: React.FC = () => {
  const { adminFilmsCache, setAdminFilmsCache } = useCache();
  const [films, setFilms] = useState<Movie[]>(adminFilmsCache?.films || []);
  const [isLoading, setIsLoading] = useState(!adminFilmsCache);
  const [isFetchingMore, setIsFetchingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(adminFilmsCache?.page || 0);
  const [hasMore, setHasMore] = useState(adminFilmsCache ? adminFilmsCache.hasMore : true);
  const navigate = useNavigate();
  const { showToast } = useToast();

  const observer = useRef<IntersectionObserver | null>(null);
  const lastElementRef = useCallback((node: HTMLTableRowElement | null) => {
    if (isLoading || isFetchingMore) return;
    if (observer.current) observer.current.disconnect();

    observer.current = new IntersectionObserver(entries => {
      if (entries[0].isIntersecting && hasMore) {
        setPage(prev => prev + 1);
      }
    }, {
      rootMargin: '1px', // Kích hoạt khi cách đáy 100px
      threshold: 0.1
    });

    if (node) observer.current.observe(node);
  }, [isLoading, isFetchingMore, hasMore]);

  useEffect(() => {
    fetchFilms();
  }, [page]);

  const fetchFilms = async () => {
    // Nếu đã có cache và đang ở page của cache thì không fetch lại
    if (adminFilmsCache && page <= adminFilmsCache.page && films.length > 0) {
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

      let updatedFilms: Movie[];
      if (page === 0) {
        updatedFilms = pageResponse.data;
      } else {
        updatedFilms = [...films, ...pageResponse.data];
      }
      
      setFilms(updatedFilms);
      setHasMore(newHasMore);

      // Cập nhật cache
      setAdminFilmsCache({
        films: updatedFilms,
        page: page,
        hasMore: newHasMore
      });
    } catch (err: any) {
      const message = 'Không thể tải danh sách phim.';
      setError(message);
      showToast(message, 'error');
    } finally {
      setIsLoading(false);
      setIsFetchingMore(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (window.confirm('Bạn có chắc chắn muốn xóa phim này?')) {
      try {
        await movieService.deleteMovie(id);
        const updatedFilms = films.filter(f => f.idMovie !== id);
        setFilms(updatedFilms);
        
        // Cập nhật cache sau khi xóa
        setAdminFilmsCache({
          films: updatedFilms,
          page: page,
          hasMore: hasMore
        });

        showToast('Xóa phim thành công!', 'success');
      } catch (err: any) {
        const message = 'Xóa phim thất bại: ' + (err.response?.data?.message || 'Lỗi không xác định');
        showToast(message, 'error');
      }
    }
  };

  const filteredFilms = films.filter(movie => 
    movie.nameMovie.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (isLoading && page === 0) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh]">
        <Loader2 className="w-12 h-12 text-green-500 animate-spin" />
        <p className="mt-4 text-neutral-400">Đang tải danh sách phim...</p>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold text-white">Quản lý phim</h1>
          <p className="text-neutral-400">Thêm, sửa, xóa các bộ phim trong hệ thống</p>
        </div>
        <Link 
          to="/admin/movies/add" 
          className="flex items-center gap-2 px-6 py-3 bg-green-600 hover:bg-green-700 text-white font-bold rounded-xl transition-all shadow-lg shadow-green-600/20"
        >
          <Plus className="w-5 h-5" />
          Thêm phim mới
        </Link>
      </div>

      <div className="bg-neutral-900 rounded-2xl border border-neutral-800 p-6 shadow-2xl">
        <div className="relative mb-6">
          <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none text-neutral-500">
            <Search className="w-5 h-5" />
          </div>
          <input
            type="text"
            placeholder="Tìm kiếm phim theo tên..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="block w-full pl-10 pr-3 py-3 bg-neutral-800 border border-neutral-700 rounded-xl focus:ring-2 focus:ring-green-500 focus:border-transparent text-white placeholder-neutral-500 outline-none transition-all"
          />
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-neutral-800 text-neutral-400 text-sm uppercase tracking-wider">
                <th className="px-4 py-4 font-medium">Phim</th>
                <th className="px-4 py-4 font-medium">Thể loại</th>
                <th className="px-4 py-4 font-medium">Thời lượng</th>
                <th className="px-4 py-4 font-medium text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-800">
              {filteredFilms.map((movie, index) => (
                <tr 
                  key={movie.idMovie} 
                  ref={index === filteredFilms.length - 1 ? lastElementRef : null}
                  className="hover:bg-neutral-800/30 transition-colors group"
                >
                  <td className="px-4 py-4">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-16 rounded overflow-hidden flex-shrink-0 bg-neutral-800">
                        {movie.image ? (
                          <img src={movie.image} alt={movie.nameMovie} className="w-full h-full object-cover" />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center">
                            <FilmIcon className="w-6 h-6 text-neutral-600" />
                          </div>
                        )}
                      </div>
                      <span className="font-bold text-white group-hover:text-green-500 transition-colors">
                        {movie.nameMovie}
                      </span>
                    </div>
                  </td>
                  <td className="px-4 py-4">
                    <div className="flex flex-wrap gap-1">
                      {movie.categories?.map((cat, i) => (
                        <span key={i} className="text-[10px] px-2 py-0.5 bg-neutral-800 border border-neutral-700 rounded text-neutral-400">
                          {cat.idCategory}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td className="px-4 py-4 text-neutral-400">{movie.duration} phút</td>
                  <td className="px-4 py-4 text-right">
                    <div className="flex justify-end gap-2">
                      <button 
                        onClick={() => navigate(`/admin/movies/edit/${movie.idMovie}`)}
                        className="p-2 bg-blue-500/10 hover:bg-blue-500/20 text-blue-500 rounded-lg transition-colors"
                        title="Chỉnh sửa"
                      >
                        <Edit className="w-5 h-5" />
                      </button>
                      <button 
                        onClick={() => handleDelete(movie.idMovie)}
                        className="p-2 bg-red-500/10 hover:bg-red-500/20 text-red-500 rounded-lg transition-colors"
                        title="Xóa"
                      >
                        <Trash2 className="w-5 h-5" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {filteredFilms.length === 0 && !isLoading && (
                <tr>
                  <td colSpan={4} className="px-4 py-12 text-center text-neutral-500 italic">
                    Không tìm thấy bộ phim nào phù hợp.
                  </td>
                </tr>
              )}
              {isFetchingMore && (
                <tr>
                  <td colSpan={4} className="px-4 py-8 text-center">
                    <Loader2 className="w-6 h-6 text-green-500 animate-spin mx-auto" />
                  </td>
                </tr>
              )}
              {!hasMore && films.length > 0 && (
                <tr>
                  <td colSpan={4} className="px-4 py-8 text-center text-neutral-500 text-sm italic">
                    Bạn đã xem hết danh sách phim.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default AdminFilms;
