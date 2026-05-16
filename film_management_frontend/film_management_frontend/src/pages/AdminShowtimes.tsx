import React, { useEffect, useState, useRef, useCallback } from 'react';
import { movieService } from '../services/movie.service';
import { type Showtime, type Movie, type Room, type AddShowtimeRequest } from '../types';
import { useToast } from '../components/Toast';
import { Loader2, Trash2, Calendar, Clock, Film, MapPin, Search, Plus, X, Check } from 'lucide-react';
import { format } from 'date-fns';
import { useCache } from '../context/CacheContext';

const AdminShowtimes: React.FC = () => {
  const { adminShowtimesCache, setAdminShowtimesCache } = useCache();
  const [showtimes, setShowtimes] = useState<Showtime[]>(adminShowtimesCache?.showtimes || []);
  const [movies, setMovies] = useState<Movie[]>(adminShowtimesCache?.movies || []);
  const [rooms, setRooms] = useState<Room[]>(adminShowtimesCache?.rooms || []);
  const [isLoading, setIsLoading] = useState(!adminShowtimesCache);
  const [isFetchingMore, setIsFetchingMore] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [timeFilter, setTimeFilter] = useState<'upcoming' | 'past'>(adminShowtimesCache?.timeFilter || 'upcoming');
  const [page, setPage] = useState(adminShowtimesCache?.page || 0);
  const [hasMore, setHasMore] = useState(adminShowtimesCache ? adminShowtimesCache.hasMore : true);
  const [showAddModal, setShowAddModal] = useState(false);
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
      rootMargin: '100px',
      threshold: 0.1
    });

    if (node) observer.current.observe(node);
  }, [isLoading, isFetchingMore, hasMore]);

  useEffect(() => {
    // Nếu filter thay đổi và khác với filter trong cache, reset page và data
    if (adminShowtimesCache && timeFilter !== adminShowtimesCache.timeFilter) {
      setPage(0);
      setHasMore(true);
      setShowtimes([]);
    }
  }, [timeFilter]);

  useEffect(() => {
    fetchData();
  }, [timeFilter, page]);

  const fetchData = async () => {
    // Nếu đã có cache cho filter hiện tại và đang ở page của cache thì không fetch lại
    if (adminShowtimesCache && 
        timeFilter === adminShowtimesCache.timeFilter && 
        page <= adminShowtimesCache.page && 
        showtimes.length > 0) {
      setIsLoading(false);
      return;
    }

    try {
      if (page === 0) {
        setIsLoading(true);
      } else {
        setIsFetchingMore(true);
      }
      
      const [showtimesRes, moviesRes, roomsRes] = await Promise.all([
        movieService.getAllShowtimes(page, 10, timeFilter),
        page === 0 && movies.length === 0 ? movieService.getPageMovies(0, 10) : Promise.resolve(null),
        page === 0 && rooms.length === 0 ? movieService.getRooms() : Promise.resolve(null)
      ]);

      let updatedShowtimes: Showtime[];
      let updatedMovies = movies;
      let updatedRooms = rooms;

      if (page === 0) {
        updatedShowtimes = showtimesRes?.data || [];
        if (moviesRes) updatedMovies = moviesRes.data || [];
        if (roomsRes) updatedRooms = roomsRes || [];
      } else {
        updatedShowtimes = [...showtimes, ...(showtimesRes?.data || [])];
      }
      
      const newHasMore = page < (showtimesRes?.totalPages || 0) - 1;
      
      setShowtimes(updatedShowtimes);
      setMovies(updatedMovies);
      setRooms(updatedRooms);
      setHasMore(newHasMore);

      // Cập nhật cache
      setAdminShowtimesCache({
        showtimes: updatedShowtimes,
        movies: updatedMovies,
        rooms: updatedRooms,
        page: page,
        hasMore: newHasMore,
        timeFilter: timeFilter
      });
    } catch (err: any) {
      showToast('Không thể tải dữ liệu lịch chiếu.', 'error');
    } finally {
      setIsLoading(false);
      setIsFetchingMore(false);
    }
  };

  const [newShowtime, setNewShowtime] = useState<AddShowtimeRequest>({
    idMovie: '',
    idRoom: '',
    showTime: '',
    standardPrice: 0,
    vipPrice: 0,
    couplePrice: 0
  });

  const handleAddShowtime = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newShowtime.idMovie || !newShowtime.idRoom || !newShowtime.showTime) {
      showToast('Vui lòng điền đầy đủ thông tin.', 'error');
      return;
    }

    if (newShowtime.standardPrice < 0 || newShowtime.vipPrice < 0 || newShowtime.couplePrice < 0) {
      showToast('Giá vé không được nhỏ hơn 0.', 'error');
      return;
    }

    try {
      setIsSubmitting(true);
      const formattedShowtime: AddShowtimeRequest = {
        idMovie: newShowtime.idMovie,
        idRoom: newShowtime.idRoom,
        showTime: new Date(newShowtime.showTime).toISOString(),
        standardPrice: newShowtime.standardPrice,
        vipPrice: newShowtime.vipPrice,
        couplePrice: newShowtime.couplePrice
      };
      await movieService.createShowtime(formattedShowtime);
      showToast('Thêm suất chiếu thành công!', 'success');
      setShowAddModal(false);
      setNewShowtime({ 
        idMovie: '', 
        idRoom: '', 
        showTime: '', 
        standardPrice: 0, 
        vipPrice: 0, 
        couplePrice: 0 
      });
      
      // Xóa cache để fetch lại dữ liệu mới sau khi thêm
      setAdminShowtimesCache(null);
      setPage(0);
      fetchData();
    } catch (err: any) {
      showToast('Không thể thêm suất chiếu. Vui lòng kiểm tra lại.', 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (window.confirm('Bạn có chắc chắn muốn xóa suất chiếu này?')) {
      try {
        await movieService.deleteShowtime(id);
        const updatedShowtimes = showtimes.filter(s => s.idShowtime !== id);
        setShowtimes(updatedShowtimes);
        
        // Cập nhật cache sau khi xóa
        setAdminShowtimesCache({
          showtimes: updatedShowtimes,
          movies: movies,
          rooms: rooms,
          page: page,
          hasMore: hasMore,
          timeFilter: timeFilter
        });

        showToast('Xóa suất chiếu thành công!', 'success');
      } catch (err: any) {
        const errorMessage = err.response?.data?.message || 'Xóa suất chiếu thất bại.';
        showToast(errorMessage, 'error');
      }
    }
  };

  const getMovieName = (movieId: string) => {
    return movies.find(m => m.idMovie === movieId)?.nameMovie || 'Phim không xác định';
  };

  const getMovieImage = (movieId: string) => {
    return movies.find(m => m.idMovie === movieId)?.image;
  };

  const filteredShowtimes = showtimes.filter(st => {
    return getMovieName(st.idMovie).toLowerCase().includes(searchTerm.toLowerCase()) ||
           st.idRoom.toLowerCase().includes(searchTerm.toLowerCase());
  });

  if (isLoading && page === 0) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh]">
        <Loader2 className="w-12 h-12 text-green-500 animate-spin" />
        <p className="mt-4 text-neutral-400">Đang tải lịch chiếu...</p>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold text-white">Quản lý suất chiếu</h1>
          <p className="text-neutral-400">Xem và quản lý các lịch chiếu phim trong hệ thống</p>
        </div>
        <button 
          onClick={() => setShowAddModal(true)}
          className="flex items-center gap-2 px-6 py-3 bg-green-600 hover:bg-green-700 text-white font-bold rounded-xl transition-all shadow-lg shadow-green-600/20"
        >
          <Plus className="w-5 h-5" />
          Thêm suất chiếu mới
        </button>
      </div>

      <div className="bg-neutral-900 rounded-2xl border border-neutral-800 p-6 shadow-2xl">
        <div className="flex flex-col md:flex-row gap-4 mb-6">
          <div className="relative flex-grow">
            <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none text-neutral-500">
              <Search className="w-5 h-5" />
            </div>
            <input
              type="text"
              placeholder="Tìm kiếm theo tên phim hoặc phòng chiếu..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="block w-full pl-10 pr-3 py-3 bg-neutral-800 border border-neutral-700 rounded-xl focus:ring-2 focus:ring-green-500 focus:border-transparent text-white placeholder-neutral-500 outline-none transition-all"
            />
          </div>
          <select
            value={timeFilter}
            onChange={(e) => setTimeFilter(e.target.value as any)}
            className="px-4 py-3 bg-neutral-800 border border-neutral-700 rounded-xl text-white outline-none focus:ring-2 focus:ring-green-500 transition-all min-w-[150px]"
          >
            <option value="upcoming">Suất chiếu sắp tới</option>
            <option value="past">Suất chiếu đã qua</option>
          </select>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-neutral-800 text-neutral-400 text-sm uppercase tracking-wider">
                <th className="px-4 py-4 font-medium">Phim</th>
                <th className="px-4 py-4 font-medium">Phòng chiếu</th>
                <th className="px-4 py-4 font-medium">Ngày chiếu</th>
                <th className="px-4 py-4 font-medium">Giờ chiếu</th>
                <th className="px-4 py-4 font-medium text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-800">
              {filteredShowtimes.map((st, index) => (
                <tr 
                  key={st.idShowtime} 
                  ref={index === filteredShowtimes.length - 1 ? lastElementRef : null}
                  className="hover:bg-neutral-800/30 transition-colors group"
                >
                  <td className="px-4 py-4">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-14 rounded overflow-hidden flex-shrink-0 bg-neutral-800">
                        <img 
                          src={getMovieImage(st.idMovie)} 
                          alt="" 
                          className="w-full h-full object-cover"
                          onError={(e) => (e.currentTarget.src = 'https://via.placeholder.com/40x60?text=?')}
                        />
                      </div>
                      <span className="font-bold text-white group-hover:text-green-500 transition-colors">
                        {getMovieName(st.idMovie)}
                      </span>
                    </div>
                  </td>
                  <td className="px-4 py-4">
                    <div className="flex items-center gap-2 text-neutral-400">
                      <MapPin className="w-4 h-4 text-green-500" />
                      {st.idRoom}
                    </div>
                  </td>
                  <td className="px-4 py-4 text-neutral-400">
                    <div className="flex items-center gap-2">
                      <Calendar className="w-4 h-4 text-blue-500" />
                      {format(new Date(st.showTime), 'dd/MM/yyyy')}
                    </div>
                  </td>
                  <td className="px-4 py-4 text-neutral-400">
                    <div className="flex items-center gap-2">
                      <Clock className="w-4 h-4 text-orange-500" />
                      {format(new Date(st.showTime), 'HH:mm')}
                    </div>
                  </td>
                  <td className="px-4 py-4 text-right">
                    {new Date(st.showTime) >= new Date() && (
                      <button 
                        onClick={() => handleDelete(st.idShowtime)}
                        className="p-2 bg-red-500/10 hover:bg-red-500/20 text-red-500 rounded-lg transition-colors"
                        title="Xóa"
                      >
                        <Trash2 className="w-5 h-5" />
                      </button>
                    )}
                  </td>
                </tr>
              ))}
              {filteredShowtimes.length === 0 && !isLoading && (
                <tr>
                  <td colSpan={5} className="px-4 py-12 text-center text-neutral-500 italic">
                    Không tìm thấy suất chiếu nào phù hợp.
                  </td>
                </tr>
              )}
              {isFetchingMore && (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center">
                    <Loader2 className="w-6 h-6 text-green-500 animate-spin mx-auto" />
                  </td>
                </tr>
              )}
              {!hasMore && showtimes.length > 0 && (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-neutral-500 text-sm italic">
                    Bạn đã xem hết danh sách suất chiếu.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Add Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="bg-neutral-900 border border-neutral-800 w-full max-w-md rounded-3xl p-8 shadow-2xl space-y-6 animate-in fade-in zoom-in duration-300">
            <div className="flex justify-between items-center">
              <h3 className="text-2xl font-bold text-white">Thêm suất chiếu</h3>
              <button onClick={() => setShowAddModal(false)} className="p-2 hover:bg-neutral-800 rounded-full text-neutral-500 transition-colors">
                <X className="w-6 h-6" />
              </button>
            </div>

            <form onSubmit={handleAddShowtime} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-neutral-400 mb-2">Chọn phim</label>
                <select
                  required
                  value={newShowtime.idMovie}
                  onChange={(e) => setNewShowtime({ ...newShowtime, idMovie: e.target.value })}
                  className="w-full px-4 py-3 bg-neutral-800 border border-neutral-700 rounded-xl text-white outline-none focus:ring-2 focus:ring-green-500 transition-all"
                >
                  <option value="">-- Chọn phim --</option>
                  {movies.map(movie => (
                    <option key={movie.idMovie} value={movie.idMovie}>{movie.nameMovie}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-neutral-400 mb-2">Phòng chiếu</label>
                <select
                  required
                  value={newShowtime.idRoom}
                  onChange={(e) => setNewShowtime({ ...newShowtime, idRoom: e.target.value })}
                  className="w-full px-4 py-3 bg-neutral-800 border border-neutral-700 rounded-xl text-white outline-none focus:ring-2 focus:ring-green-500 transition-all"
                >
                  <option value="">-- Chọn phòng chiếu --</option>
                  {rooms.map(room => (
                    <option key={room.idRoom} value={room.idRoom}>{room.idRoom}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-neutral-400 mb-2">Thời gian chiếu</label>
                <input
                  type="datetime-local"
                  required
                  value={newShowtime.showTime}
                  onChange={(e) => setNewShowtime({ ...newShowtime, showTime: e.target.value })}
                  className="w-full px-4 py-3 bg-neutral-800 border border-neutral-700 rounded-xl text-white outline-none focus:ring-2 focus:ring-green-500 transition-all"
                />
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-medium text-neutral-500 mb-2 uppercase">Giá Standard</label>
                  <input
                    type="number"
                    required
                    min="0"
                    placeholder="0"
                    value={newShowtime.standardPrice || ''}
                    onChange={(e) => setNewShowtime({ ...newShowtime, standardPrice: Math.max(0, parseInt(e.target.value) || 0) })}
                    className="w-full px-3 py-2 bg-neutral-800 border border-neutral-700 rounded-lg text-white text-sm outline-none focus:ring-2 focus:ring-green-500 transition-all"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-neutral-500 mb-2 uppercase">Giá VIP</label>
                  <input
                    type="number"
                    required
                    min="0"
                    placeholder="0"
                    value={newShowtime.vipPrice || ''}
                    onChange={(e) => setNewShowtime({ ...newShowtime, vipPrice: Math.max(0, parseInt(e.target.value) || 0) })}
                    className="w-full px-3 py-2 bg-neutral-800 border border-neutral-700 rounded-lg text-white text-sm outline-none focus:ring-2 focus:ring-green-500 transition-all"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-neutral-500 mb-2 uppercase">Giá Couple</label>
                  <input
                    type="number"
                    required
                    min="0"
                    placeholder="0"
                    value={newShowtime.couplePrice || ''}
                    onChange={(e) => setNewShowtime({ ...newShowtime, couplePrice: Math.max(0, parseInt(e.target.value) || 0) })}
                    className="w-full px-3 py-2 bg-neutral-800 border border-neutral-700 rounded-lg text-white text-sm outline-none focus:ring-2 focus:ring-green-500 transition-all"
                  />
                </div>
              </div>

              <div className="pt-4 flex gap-3">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="flex-1 px-6 py-3 bg-neutral-800 hover:bg-neutral-700 text-white font-bold rounded-xl transition-all"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="flex-[2] px-6 py-3 bg-green-600 hover:bg-green-700 text-white font-bold rounded-xl shadow-lg shadow-green-600/20 disabled:opacity-50 transition-all flex items-center justify-center gap-2"
                >
                  {isSubmitting ? <Loader2 className="w-5 h-5 animate-spin" /> : <Check className="w-5 h-5" />}
                  Xác nhận
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminShowtimes;

