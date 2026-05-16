import React, { useEffect, useState } from 'react';
import { Search, Loader2, Calendar, User, CreditCard, XCircle } from 'lucide-react';
import { bookingService } from '../services/ticket.service';
import { movieService } from '../services/movie.service';
import { type Showtime, type Movie, type Bill, type Ticket } from '../types';
import { useToast } from '../components/Toast';

interface BillWithDetails extends Bill {
  movieName?: string;
  showtime?: Showtime;
  tickets?: Ticket[];
}

const AdminBookings: React.FC = () => {
  const [bills, setBills] = useState<BillWithDetails[]>([]);
  const [showtimes, setShowtimes] = useState<Showtime[]>([]);
  const [movies, setMovies] = useState<Movie[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedShowtime, setSelectedShowtime] = useState<string>('all');
  const [searchTerm, setSearchTerm] = useState('');
  const { showToast } = useToast();

  useEffect(() => {
    fetchInitialData();
  }, []);

  const fetchInitialData = async () => {
    try {
      setIsLoading(true);
      const [billsData, showtimesData, pageResponse] = await Promise.all([
        bookingService.getAllBills(),
        movieService.getAllShowtimes(0, 10),
        movieService.getPageMovies(0, 10)
      ]);

      // Lấy chi tiết từng bill để có thông tin vé và suất chiếu
      const billsWithDetails = await Promise.all(
        (billsData || []).map(async (bill) => {
          try {
            const detail = await bookingService.getBillDetail(bill.idBill);
            
            return {
              ...bill,
              tickets: detail.tickets,
            };
          } catch (e) {
            return bill;
          }
        })
      );

      setBills(billsWithDetails);
      setShowtimes(showtimesData?.data || []);
      setMovies(pageResponse?.data || []);
    } catch (err: any) {
      showToast('Không thể tải dữ liệu đặt vé.', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  const getMovieName = (movieId: string) => {
    return movies.find(m => m.idMovie === movieId)?.nameMovie || 'Phim không xác định';
  };

  const getShowtimeInfo = (bill: BillWithDetails) => {
    if (!bill.tickets || bill.tickets.length === 0) return null;
    const showtimeId = bill.tickets[0].idShowtime;
    const st = showtimes.find(s => s.idShowtime === showtimeId);
    if (!st) return null;
    
    const movie = movies.find(m => m.idMovie === st.idMovie);
    return {
      movieName: movie?.nameMovie || 'Phim không xác định',
      time: new Date(st.showTime).toLocaleString('vi-VN', { 
        hour: '2-digit', 
        minute: '2-digit',
        day: '2-digit',
        month: '2-digit'
      })
    };
  };

  const handleCancel = async (idBill: string) => {
    if (window.confirm('Bạn có chắc chắn muốn hủy hóa đơn này?')) {
      try {
        await bookingService.cancelBooking(idBill);
        showToast('Hủy hóa đơn thành công!', 'success');
        fetchInitialData();
      } catch (err: any) {
        showToast('Hủy hóa đơn thất bại.', 'error');
      }
    }
  };

  const filteredBills = bills.filter(bill => {
    const matchesShowtime = selectedShowtime === 'all' || 
                           bill.tickets?.some(t => t.idShowtime === selectedShowtime);
    
    const matchesSearch = bill.idBill.toLowerCase().includes(searchTerm.toLowerCase()) || 
                         bill.idAccount.toLowerCase().includes(searchTerm.toLowerCase());
                         
    return matchesShowtime && matchesSearch;
  });

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh]">
        <Loader2 className="w-12 h-12 text-green-500 animate-spin" />
        <p className="mt-4 text-neutral-400">Đang tải danh sách hóa đơn...</p>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold text-white">Quản lý đặt vé</h1>
          <p className="text-neutral-400">Xem và quản lý tất cả hóa đơn đặt vé trong hệ thống</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-neutral-500" />
          <input
            type="text"
            placeholder="Tìm theo mã hóa đơn hoặc ID khách hàng..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-neutral-800 border border-neutral-700 rounded-xl text-white outline-none focus:ring-2 focus:ring-green-500"
          />
        </div>
        <select
          value={selectedShowtime}
          onChange={(e) => setSelectedShowtime(e.target.value)}
          className="px-4 py-2 bg-neutral-800 border border-neutral-700 rounded-xl text-white outline-none focus:ring-2 focus:ring-green-500"
        >
          <option value="all">Tất cả suất chiếu</option>
          {showtimes.map(st => (
            <option key={st.idShowtime} value={st.idShowtime}>
              {new Date(st.showTime).toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' })} - {getMovieName(st.idMovie)}
            </option>
          ))}
        </select>
      </div>

      <div className="space-y-4">
        {filteredBills.map((bill) => {
          const info = getShowtimeInfo(bill);
          return (
            <div key={bill.idBill} className="bg-neutral-900 rounded-2xl border border-neutral-800 p-6 hover:border-neutral-700 transition-all shadow-xl">
              <div className="flex flex-col lg:flex-row justify-between gap-6">
                <div className="flex-grow space-y-4">
                  <div className="flex flex-wrap items-center justify-between gap-4">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-xl bg-green-500/10 flex items-center justify-center text-green-500">
                        <User className="w-5 h-5" />
                      </div>
                      <div>
                        <p className="text-xs text-neutral-500 font-bold uppercase">Khách hàng ID</p>
                        <p className="text-white font-mono">{bill.idAccount}</p>
                      </div>
                    </div>

                    {info && (
                      <div className="px-4 py-2 bg-neutral-800 rounded-xl border border-neutral-700">
                        <p className="text-xs text-neutral-500 font-bold uppercase">Phim & Suất chiếu</p>
                        <p className="text-sm text-white font-semibold">
                          {info.movieName} <span className="text-green-500 ml-2">({info.time})</span>
                        </p>
                      </div>
                    )}

                    <div className="text-right">
                      <p className="text-xs text-neutral-500 font-bold uppercase">Mã hóa đơn</p>
                      <p className="text-green-500 font-mono font-bold">{bill.idBill}</p>
                    </div>
                  </div>

                <div className="grid grid-cols-2 md:grid-cols-3 gap-4 pt-4 border-t border-neutral-800">
                  <div className="space-y-1">
                    <p className="text-xs text-neutral-500 font-bold uppercase tracking-wider">Thời gian đặt</p>
                    <div className="flex items-center gap-2 text-sm text-neutral-200">
                      <Calendar className="w-4 h-4 text-neutral-500" />
                      {new Date(bill.createdAt).toLocaleString('vi-VN', { 
                        day: '2-digit', 
                        month: '2-digit', 
                        year: 'numeric', 
                        hour: '2-digit', 
                        minute: '2-digit' 
                      })}
                    </div>
                  </div>
                  <div className="space-y-1">
                    <p className="text-xs text-neutral-500 font-bold uppercase tracking-wider">Tổng tiền</p>
                    <div className="flex items-center gap-2 text-base font-bold text-green-500">
                      <CreditCard className="w-4.5 h-4.5" />
                      {bill.totalAmount.toLocaleString('vi-VN')}đ
                    </div>
                  </div>
                  <div className="space-y-1 text-right md:text-left">
                    <p className="text-xs text-neutral-500 font-bold uppercase tracking-wider">Số lượng vé</p>
                    <div className="flex items-center gap-2 text-sm text-neutral-200 justify-end md:justify-start">
                      <svg className="w-4 h-4 text-neutral-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 5v2m0 4v2m0 4v2M5 5a2 2 0 00-2 2v3a2 2 0 002 2h14a2 2 0 002-2v-3a2 2 0 00-2-2H5z" /></svg>
                      {bill.tickets?.length || 0} vé
                    </div>
                  </div>
                </div>

                {bill.tickets && bill.tickets.length > 0 && (
                  <div className="pt-4 flex flex-wrap gap-2">
                    {bill.tickets.map((ticket, idx) => (
                      <span key={idx} className="px-3 py-1.5 bg-neutral-800 border border-neutral-700 rounded-lg text-xs font-medium text-neutral-200 flex items-center gap-2">
                        <span className="w-1.5 h-1.5 rounded-full bg-green-500"></span>
                        Ghế: <span className="font-bold text-white">{ticket.seatCode}</span>
                        <span className="text-neutral-500">|</span>
                        <span className="text-green-500">{ticket.price.toLocaleString('vi-VN')}đ</span>
                      </span>
                    ))}
                  </div>
                )}
              </div>

              <div className="flex items-center justify-center lg:pl-6 lg:border-l border-neutral-800">
                <button
                  onClick={() => handleCancel(bill.idBill)}
                  className="flex items-center gap-2 px-4 py-2 bg-red-500/10 hover:bg-red-500/20 text-red-500 text-sm font-bold rounded-lg transition-all"
                >
                  <XCircle className="w-4 h-4" />
                  Hủy hóa đơn
                </button>
              </div>
            </div>
          </div>
        );
      })}

        {filteredBills.length === 0 && (
          <div className="bg-neutral-900 rounded-2xl border border-dashed border-neutral-800 p-16 text-center">
            <svg className="w-16 h-16 text-neutral-800 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 5v2m0 4v2m0 4v2M5 5a2 2 0 00-2 2v3a2 2 0 002 2h14a2 2 0 002-2v-3a2 2 0 00-2-2H5z" />
            </svg>
            <p className="text-neutral-500 italic">Không tìm thấy hóa đơn nào.</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminBookings;
