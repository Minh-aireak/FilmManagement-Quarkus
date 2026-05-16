import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Info, Loader2 } from 'lucide-react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { bookingService } from '../services/ticket.service';
import { authService } from '../services/auth.service';
import { movieService } from '../services/movie.service';
import { type Showtime, type Seat, type Movie, type BookingResponseDTO } from '../types';
import { useToast } from '../components/Toast';
import { format } from 'date-fns';
import { vi } from 'date-fns/locale';
import { CheckCircle2, Ticket as TicketIcon, CreditCard, Download, Calendar, Clock, MapPin } from 'lucide-react';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

interface RoomConfig {
  name: string;
  columns: number;
  aisles: number[];
  description: string;
}

const ROOM_CONFIGS: Record<string | number, RoomConfig> = {
  1: {
    name: "Phòng Standard - Room 1",
    columns: 5,
    aisles: [],
    description: "Phòng chiếu 5x5, không gian tiêu chuẩn"
  },
  2: {
    name: "Phòng Comfort - Room 2",
    columns: 6,
    aisles: [],
    description: "Phòng chiếu 6x6, hàng A đặc biệt với 4 ghế"
  },
  3: {
    name: "Phòng Premium - Room 3",
    columns: 5,
    aisles: [],
    description: "Phòng chiếu 5x5, hàng A đặc biệt với 3 ghế"
  }
};

const ROWS = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K'];

const SeatSelection: React.FC = () => {
  const { showtimeId } = useParams();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [showtime, setShowtime] = useState<Showtime | null>(null);
  const [movie, setMovie] = useState<Movie | null>(null);
  const [seats, setSeats] = useState<Seat[]>([]);
  const [selectedSeats, setSelectedSeats] = useState<Seat[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentStep, setCurrentStep] = useState(1);
  const [bookingResult, setBookingResult] = useState<BookingResponseDTO | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      if (!showtimeId) return;
      try {
        setIsLoading(true);
        setError(null);
        
        // 1. Lấy danh sách ghế đã đặt
        const bookedSeatIds = await bookingService.getBookedSeats(showtimeId);
        
        // 2. Lấy thông tin suất chiếu
        const allShowtimes = await movieService.getAllShowtimes(0, 10);
        const currentShowtime = allShowtimes.data.find((st: Showtime) => st.idShowtime === showtimeId);
        
        if (!currentShowtime) {
          throw new Error('Không tìm thấy thông tin suất chiếu');
        }

        setShowtime(currentShowtime);

         // 3. Lấy thông tin phim
         const movieData = await movieService.getMovieById(currentShowtime.idMovie);
         setMovie(movieData);

         // 4. Tạo danh sách tất cả ghế dựa trên cấu hình phòng
         // Thử lấy số từ ID phòng (ví dụ "ROOM1" -> 1)
         const roomRawId = currentShowtime.idRoom;
         const roomNumericId = parseInt(roomRawId.replace(/[^0-9]/g, '')) || 1;
         
         // Lấy cấu hình phòng, nếu không có thì lấy mặc định là phòng 1
         const config = ROOM_CONFIGS[roomNumericId] || ROOM_CONFIGS[1];
         const roomId = roomNumericId;

         const allSeats: Seat[] = [];
         const rows = roomId === 2 ? ['A', 'B', 'C', 'D', 'E', 'F'] : ['A', 'B', 'C', 'D', 'E'];
         
         rows.forEach(row => {
           for (let col = 1; col <= config.columns; col++) {
             const seatId = `${row}${col}`;
             
             let seatType: 'STANDARD' | 'VIP' | 'DOUBLE' = 'STANDARD';
             if (roomId === 1) {
               if (row === 'E' && col <= 4) seatType = 'DOUBLE';
               else if (row === 'C' || row === 'D') seatType = 'VIP';
             } else if (roomId === 2) {
               if (row === 'F') seatType = 'DOUBLE';
               else if (row === 'A' || row === 'D' || row === 'E') seatType = 'VIP';
             } else if (roomId === 3) {
               if (row === 'E' && col <= 4) seatType = 'DOUBLE';
               else if (row === 'A' || row === 'D') seatType = 'VIP';
             }

             allSeats.push({
               id: seatId,
               roomId: currentShowtime.idRoom.toString(),
               row: row,
               column: col,
               type: seatType,
               isBooked: bookedSeatIds.includes(seatId)
             });
           }
         });

        setSeats(allSeats);
      } catch (err: any) {
        const message = 'Không thể tải sơ đồ ghế. Vui lòng thử lại sau.';
        setError(message);
        showToast(message, 'error');
      } finally {
        setIsLoading(false);
      }
    };

    fetchData();
  }, [showtimeId]);

  const toggleSeat = (seat: Seat) => {
    if (seat.isBooked) return;
    setSelectedSeats(prev => 
      prev.find(s => s.id === seat.id) 
        ? prev.filter(s => s.id !== seat.id) 
        : [...prev, seat]
    );
  };

  const calculateTotal = () => {
    return selectedSeats.reduce((total, seat) => {
      // Giả sử giá cơ bản là 1, bạn có thể thay đổi tùy logic backend
      let price = 100000; // Giá mặc định nếu không lấy được từ showtime
      if (seat.type === 'VIP') price += 30000;
      if (seat.type === 'DOUBLE') price += 100000;
      return total + price;
    }, 0);
  };

  const handleBooking = async () => {
    if (selectedSeats.length === 0 || !showtime) return;
    
    const loginData = authService.getCurrentUser();
    if (!loginData || !loginData.token) {
      showToast('Vui lòng đăng nhập để đặt vé', 'info');
      navigate('/login');
      return;
    }
    
    try {
      setIsLoading(true);
      
      const bookingData = {
        idShowtime: showtime.idShowtime,
        seatCodes: selectedSeats.map(s => s.id)
      };

      const response = await bookingService.bookTickets(bookingData);
      showToast('Đặt vé thành công!', 'success');
      setBookingResult(response);
      setCurrentStep(2);
    } catch (err: any) {
      const message = err.response?.data?.message || 'Đặt vé thất bại. Vui lòng thử lại.';
      showToast(message, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[50vh] space-y-4">
        <Loader2 className="w-12 h-12 text-green-500 animate-spin" />
        <p className="text-neutral-400 animate-pulse">Đang tải sơ đồ ghế...</p>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Progress Bar */}
      <div className="flex items-center bg-neutral-900 rounded-full overflow-hidden border border-neutral-800">
        <div className={cn(
          "flex-1 flex items-center justify-center py-3 gap-2 font-bold italic transition-all duration-500",
          currentStep === 1 ? "bg-green-600 text-white" : "bg-neutral-800 text-neutral-500"
        )}>
          <span className={cn(
            "w-6 h-6 rounded-full flex items-center justify-center text-xs not-italic",
            currentStep === 1 ? "bg-white text-green-600" : "bg-neutral-700 text-neutral-400"
          )}>1</span>
          Chọn ghế
        </div>
        <div className={cn(
          "w-0 h-0 border-y-[24px] border-y-transparent border-l-[24px] transition-all duration-500",
          currentStep === 1 ? "border-l-green-600" : "border-l-neutral-800"
        )} />
        <div className={cn(
          "flex-1 flex items-center justify-center py-3 gap-2 font-bold italic transition-all duration-500",
          currentStep === 2 ? "bg-green-600 text-white" : "bg-neutral-900 text-neutral-500"
        )}>
          <span className={cn(
            "w-6 h-6 rounded-full flex items-center justify-center text-xs not-italic",
            currentStep === 2 ? "bg-white text-green-600" : "bg-neutral-800 text-neutral-500"
          )}>2</span>
          Thành công
        </div>
      </div>

      {currentStep === 1 ? (
        <div className="flex flex-col lg:flex-row gap-8 min-h-[80vh]">
      {/* Left side: Seat Map */}
      <div className="flex-grow bg-neutral-900 rounded-2xl border border-neutral-800 p-8 shadow-2xl">
        <div className="flex items-center gap-4 mb-8 pb-4 border-b border-neutral-800 overflow-x-auto">
          <div className="flex items-center gap-4 text-sm font-medium whitespace-nowrap">
            <div className="flex items-center gap-2">
              <div className="w-6 h-6 bg-orange-500 rounded-sm border border-orange-400" />
              <span>Ghế đang chọn</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-6 h-6 bg-neutral-700 rounded-sm border border-neutral-600" />
              <span>Ghế có thể chọn</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-6 h-6 bg-neutral-900 rounded-sm border-2 border-red-900" />
              <span>Ghế VIP</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-12 h-5 bg-neutral-700 rounded-sm border border-neutral-600" />
              <span className="text-xs">Ghế đôi (Couple)</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-6 h-6 bg-orange-700/50 rounded-sm border border-orange-800/50" />
              <span>Ghế đã bán</span>
            </div>
          </div>
        </div>

        {/* Screen */}
        <div className="relative mb-20">
          <div className="w-full h-2 bg-gradient-to-b from-neutral-600 to-transparent rounded-full blur-sm opacity-50" />
          <div className="w-[80%] h-8 bg-neutral-800 mx-auto rounded-b-[100px] border-t-4 border-red-950 flex items-center justify-center shadow-[0_10px_30px_rgba(255,255,255,0.05)]">
            <span className="text-white font-bold tracking-[0.5em] text-sm">SCREEN</span>
          </div>
        </div>

        {/* Seats Grid */}
        <div className="flex flex-col gap-3 items-center select-none overflow-x-auto py-4">
          {ROWS.map(rowLabel => {
            const rowSeats = seats.filter(s => s.row === rowLabel).sort((a, b) => a.column - b.column);
            if (rowSeats.length === 0) return null;

            return (
              <div key={rowLabel} className="flex gap-3 items-center">
                <span className="w-6 text-center text-xs font-bold text-neutral-600">{rowLabel}</span>
                <div className="flex gap-2">
                  {rowSeats.map((seat) => {
                    const isBooked = seat.isBooked;
                    const isSelected = !!selectedSeats.find(s => s.id === seat.id);
                    const isVIP = seat.type === 'VIP';
                    const isDouble = seat.type === 'DOUBLE';

                    return (
                      <button
                        key={seat.id}
                        disabled={isBooked}
                        onClick={() => toggleSeat(seat)}
                        className={cn(
                          "relative flex items-center justify-center text-[10px] font-bold transition-all duration-200 rounded-sm",
                          isDouble ? "w-[72px] h-8" : "w-8 h-8",
                          isBooked 
                            ? "bg-orange-900/30 border border-orange-900/50 text-orange-900/50 cursor-not-allowed"
                            : isSelected
                              ? "bg-orange-500 border border-orange-400 text-white shadow-[0_0_15px_rgba(249,115,22,0.4)]"
                              : isVIP
                                ? "bg-neutral-800 border-2 border-red-900 hover:border-green-500 text-neutral-400"
                                : "bg-neutral-700 border border-neutral-600 hover:border-green-500 text-neutral-400"
                        )}
                      >
                        {seat.column}
                        {isVIP && !isSelected && !isBooked && (
                          <div className="absolute -top-1 -right-1 w-2 h-2 bg-red-600 rounded-full" />
                        )}
                      </button>
                    );
                  })}
                </div>
                <span className="w-6 text-center text-xs font-bold text-neutral-600">{rowLabel}</span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Right side: Summary */}
      <div className="w-full lg:w-96 space-y-6">
        <div className="bg-neutral-900 rounded-2xl border border-neutral-800 p-6 shadow-2xl sticky top-24">
          <div className="space-y-4 mb-6">
            <h2 className="text-xl font-bold text-green-500 leading-tight uppercase">
              {movie?.nameMovie}
            </h2>
            <div className="space-y-1 text-sm text-neutral-400">
              <p>
                {showtime && format(new Date(showtime.showTime), 'HH:mm')} - {showtime && format(new Date(showtime.showTime), 'EEEE, dd/MM/yyyy', { locale: vi })}
              </p>
              <p>Aireak Cinema Hà Nội - {showtime?.idRoom} - Hà Nội</p>
            </div>
          </div>

          <div className="border-t border-neutral-800 pt-6 space-y-4">
            <h3 className="text-xs font-bold uppercase tracking-wider text-neutral-500 bg-neutral-800/50 px-3 py-2 rounded">
              ĐƠN HÀNG CỦA BẠN
            </h3>
            
            <div className="space-y-3">
              {selectedSeats.length > 0 ? (
                <div className="flex flex-col gap-2">
                  <p className="text-sm font-medium text-green-500">
                    {selectedSeats.map(s => `${s.row}${s.column}`).join(', ')}
                  </p>
                  <div className="flex justify-between items-center text-lg font-bold">
                    <span>Tổng cộng:</span>
                    <span className="text-green-500">{calculateTotal().toLocaleString('vi-VN')}đ</span>
                  </div>
                </div>
              ) : (
                <p className="text-sm text-neutral-500 italic text-center py-4">
                  Vui lòng chọn ghế để tiếp tục
                </p>
              )}
            </div>
          </div>

          <button
            disabled={selectedSeats.length === 0 || isLoading}
            onClick={handleBooking}
            className="w-full mt-8 py-4 bg-green-600 hover:bg-green-700 disabled:bg-neutral-800 disabled:text-neutral-600 text-white font-bold rounded-xl transition-all shadow-lg shadow-green-600/20 active:scale-95 flex items-center justify-center gap-2"
          >
            {isLoading ? <Loader2 className="w-5 h-5 animate-spin" /> : 'Xác nhận đặt vé'}
          </button>
        </div>
      </div>
      </div>
    ) : (
      /* Step 2: Success UI */
        <div className="max-w-3xl mx-auto space-y-8 pb-20 animate-in fade-in slide-in-from-bottom-4 duration-700">
          <div className="text-center space-y-4">
            <div className="inline-flex items-center justify-center w-20 h-20 bg-green-500/10 rounded-full mb-4">
              <CheckCircle2 className="w-12 h-12 text-green-500" />
            </div>
            <h1 className="text-4xl font-black text-white italic tracking-tighter uppercase">Đặt vé thành công!</h1>
            <p className="text-neutral-400">Cảm ơn bạn đã lựa chọn Aireak Cinema. Chúc bạn có những phút giây xem phim vui vẻ!</p>
          </div>

          <div className="bg-neutral-900 rounded-3xl border border-neutral-800 overflow-hidden shadow-2xl">
            <div className="p-8 border-b border-neutral-800 bg-gradient-to-r from-green-500/5 to-transparent">
              <div className="flex flex-col md:flex-row gap-8">
                <div className="w-full md:w-40 aspect-[2/3] rounded-xl overflow-hidden shadow-lg border border-neutral-800 flex-shrink-0">
                  <img 
                    src={movie?.image} 
                    alt={movie?.nameMovie} 
                    className="w-full h-full object-cover"
                  />
                </div>
                <div className="flex-grow space-y-6">
                  <div>
                    <h2 className="text-3xl font-black text-white uppercase tracking-tight mb-2">{movie?.nameMovie}</h2>
                    <div className="flex flex-wrap gap-4 text-sm text-neutral-400">
                      <span className="flex items-center gap-1.5"><Clock className="w-4 h-4 text-green-500" /> {movie?.duration} phút</span>
                      <span className="flex items-center gap-1.5"><MapPin className="w-4 h-4 text-green-500" /> Aireak Cinema - {showtime?.idRoom}</span>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-6 p-6 bg-neutral-800/30 rounded-2xl border border-neutral-700/50">
                    <div className="space-y-1">
                      <span className="text-[10px] uppercase font-bold text-neutral-500 tracking-widest">Ngày chiếu</span>
                      <div className="flex items-center gap-2 text-white font-bold">
                        <Calendar className="w-4 h-4 text-green-500" />
                        {showtime && format(new Date(showtime.showTime), 'dd/MM/yyyy')}
                      </div>
                    </div>
                    <div className="space-y-1">
                      <span className="text-[10px] uppercase font-bold text-neutral-500 tracking-widest">Giờ chiếu</span>
                      <div className="flex items-center gap-2 text-white font-bold">
                        <Clock className="w-4 h-4 text-green-500" />
                        {showtime && format(new Date(showtime.showTime), 'HH:mm')}
                      </div>
                    </div>
                    <div className="space-y-1">
                      <span className="text-[10px] uppercase font-bold text-neutral-500 tracking-widest">Ghế đã chọn</span>
                      <div className="flex items-center gap-2 text-green-500 font-black italic">
                        <TicketIcon className="w-4 h-4" />
                        {bookingResult?.tickets.map(t => t.seatCode).join(', ')}
                      </div>
                    </div>
                    <div className="space-y-1">
                      <span className="text-[10px] uppercase font-bold text-neutral-500 tracking-widest">Mã hóa đơn</span>
                      <div className="text-white font-mono text-sm">#{bookingResult?.bill.idBill.slice(0, 8).toUpperCase()}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div className="p-8 space-y-6">
              <div className="pt-6 border-t border-dashed border-neutral-800 flex justify-between items-end">
                <div className="space-y-1">
                  <span className="text-xs text-neutral-500 font-bold uppercase tracking-widest">Tổng tiền thanh toán</span>
                  <div className="text-4xl font-black text-green-500 italic tracking-tighter">
                    {bookingResult?.bill.totalAmount.toLocaleString('vi-VN')}đ
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className="flex flex-col sm:flex-row gap-4">
            <button 
              onClick={() => navigate('/')}
              className="flex-1 py-4 bg-green-600 hover:bg-green-500 text-white rounded-2xl font-black italic uppercase tracking-widest text-center shadow-lg shadow-green-600/20 transition-all"
            >
              Quay lại trang chủ
            </button>
           
          </div>
        </div>
      )}
    </div>
  );
};

export default SeatSelection;
