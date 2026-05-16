import React, { useEffect, useState, useMemo } from 'react';
import { 
  Ticket, DollarSign, 
  TrendingUp, Calendar, Clock
} from 'lucide-react';
import { movieService } from '../services/movie.service';
import { bookingService } from '../services/ticket.service';
import { type Bill } from '../types';

const AdminDashboard: React.FC = () => {
  const [data, setData] = useState<{
    bills: Bill[];
    showtimes: any[];
    allMovies: any[];
  }>({
    bills: [],
    showtimes: [],
    allMovies: []
  });

  const [selectedMonth, setSelectedMonth] = useState(() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  });

  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchInitialData = async () => {
      try {
        setIsLoading(true);
        const [showtimesData, bills, moviesData] = await Promise.all([
          movieService.getAllShowtimes(0, 1000, 'all'),
          bookingService.getAllBills(),
          movieService.getPageMovies(0, 100)
        ]);

        setData({
          bills,
          showtimes: showtimesData.data || [],
          allMovies: moviesData.data || []
        });

      } catch (error) {
        console.error('Dashboard error:', error);
      } finally {
        setIsLoading(false);
      }
    };
    fetchInitialData();
  }, []);

  const monthlyStats = useMemo(() => {
    const [year, month] = selectedMonth.split('-').map(Number);

    const filteredBills = data.bills.filter(bill => {
      const date = new Date(bill.createdAt);
      return date.getFullYear() === year && (date.getMonth() + 1) === month;
    });

    const filteredShowtimes = data.showtimes.filter(st => {
      const date = new Date(st.showTime);
      return date.getFullYear() === year && (date.getMonth() + 1) === month;
    });

    const uniqueMoviesInMonth = new Set(filteredShowtimes.map(st => st.idMovie));

    // Thống kê khung giờ xem nhiều nhất dựa trên hóa đơn
    const timeSlotCounts: Record<string, number> = {
      'Sáng (08:00 - 12:00)': 0,
      'Chiều (12:00 - 17:00)': 0,
      'Tối (17:00 - 22:00)': 0,
      'Khuya (22:00 - 01:00)': 0
    };

    filteredBills.forEach(bill => {
      const hour = new Date(bill.createdAt).getHours();
      if (hour >= 8 && hour < 12) timeSlotCounts['Sáng (08:00 - 12:00)']++;
      else if (hour >= 12 && hour < 17) timeSlotCounts['Chiều (12:00 - 17:00)']++;
      else if (hour >= 17 && hour < 22) timeSlotCounts['Tối (17:00 - 22:00)']++;
      else timeSlotCounts['Khuya (22:00 - 01:00)']++;
    });

    const sortedTimeSlots = Object.entries(timeSlotCounts)
      .sort(([, a], [, b]) => b - a)
      .map(([label, count]) => ({ label, count }));

    return {
      activeMovies: uniqueMoviesInMonth.size,
      bookings: filteredBills.length,
      revenue: filteredBills.reduce((sum, b) => sum + b.totalAmount, 0),
      topTimeSlots: sortedTimeSlots
    };
  }, [data, selectedMonth]);

  const statCards = [
    { label: 'Số phim chiếu', value: monthlyStats.activeMovies, icon: Calendar, color: 'text-blue-500', bgColor: 'bg-blue-500/10' },
    { label: 'Vé đã đặt', value: monthlyStats.bookings, icon: Ticket, color: 'text-amber-500', bgColor: 'bg-amber-500/10' },
    { label: 'Tổng doanh thu', value: `${monthlyStats.revenue.toLocaleString()}đ`, icon: DollarSign, color: 'text-green-500', bgColor: 'bg-green-500/10' },
  ];

  return (
    <div className="space-y-8">
      {/* Dashboard Header & Month Picker */}
      <div className="flex flex-col lg:flex-row justify-between items-start lg:items-end gap-6 bg-gradient-to-br from-neutral-900 to-neutral-950 p-8 rounded-[2rem] border border-neutral-800 shadow-2xl relative overflow-hidden group">
        <div className="absolute top-0 right-0 w-64 h-64 bg-green-500/5 blur-[100px] -mr-32 -mt-32 rounded-full" />
        <div className="relative z-10">
          <h1 className="text-4xl font-black text-white italic uppercase tracking-tighter flex items-center gap-3">
            <TrendingUp className="w-10 h-10 text-green-500" /> Dashboard
          </h1>
          <p className="text-neutral-400 mt-2 font-medium">Thống kê hoạt động kinh doanh theo thời gian</p>
        </div>
        <div className="relative z-10 flex flex-col gap-2 w-full lg:w-auto">
          <label className="text-xs font-black text-neutral-500 uppercase tracking-[0.2em] ml-1">Chọn thời gian thống kê</label>
          <div className="flex items-center gap-3 bg-neutral-800/80 backdrop-blur-xl p-4 rounded-2xl border border-neutral-700/50 hover:border-green-500/50 transition-all shadow-inner group/input">
            <Calendar className="w-5 h-5 text-green-500 group-hover/input:scale-110 transition-transform" />
            <input 
              type="month" 
              value={selectedMonth}
              onChange={(e) => setSelectedMonth(e.target.value)}
              className="bg-transparent text-white outline-none font-black cursor-pointer appearance-none uppercase tracking-tight"
              style={{ colorScheme: 'dark' }}
            />
          </div>
        </div>
      </div>

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {statCards.map((card, index) => (
          <div key={index} className="bg-neutral-900 p-8 rounded-[2rem] border border-neutral-800 shadow-xl group hover:border-green-500/30 transition-all relative overflow-hidden">
            <div className="absolute top-0 right-0 w-32 h-32 bg-white/5 blur-3xl -mr-16 -mt-16 rounded-full group-hover:bg-green-500/10 transition-colors" />
            <div className="flex justify-between items-start mb-6 relative z-10">
              <div className={`p-4 rounded-2xl ${card.bgColor} group-hover:scale-110 transition-transform`}>
                <card.icon className={`w-7 h-7 ${card.color}`} />
              </div>
              <div className="text-[10px] font-black text-neutral-600 uppercase tracking-widest bg-neutral-800 px-3 py-1 rounded-full border border-neutral-700">Tháng {selectedMonth.split('-')[1]}</div>
            </div>
            <div className="relative z-10">
              <p className="text-neutral-500 text-sm font-bold uppercase tracking-wider">{card.label}</p>
              <h3 className="text-4xl font-black text-white mt-2 italic tracking-tighter group-hover:text-green-500 transition-colors">{card.value}</h3>
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Phim xem nhiều nhất */}
        <div className="bg-neutral-900 rounded-[2.5rem] border border-neutral-800 p-10 shadow-2xl relative overflow-hidden">
          <div className="absolute top-0 left-0 w-64 h-64 bg-green-500/5 blur-[100px] -ml-32 -mt-32 rounded-full" />
          <h3 className="text-2xl font-black text-white mb-8 flex items-center gap-3 relative z-10 italic uppercase tracking-tighter">
            <TrendingUp className="w-6 h-6 text-green-500" /> Phim xem nhiều nhất
          </h3>
          <div className="space-y-8 relative z-10">
            {[1, 2, 3, 4, 5].map((_, i) => (
              <div key={i} className="flex items-center gap-6 group/item">
                <div className="w-20 h-24 rounded-2xl bg-neutral-800 flex-shrink-0 overflow-hidden border border-neutral-700 group-hover/item:border-green-500/50 transition-all shadow-lg">
                   <div className="w-full h-full bg-neutral-700 animate-pulse" />
                </div>
                <div className="flex-grow">
                  <div className="flex justify-between items-end mb-2">
                    <p className="text-lg font-black text-white uppercase italic tracking-tighter group-hover/item:text-green-500 transition-colors">Avatar: The Way of Water</p>
                    <span className="text-sm font-black text-green-500 italic">{(2500 - i * 400).toLocaleString()} vé</span>
                  </div>
                  <div className="w-full bg-neutral-800/50 h-3 rounded-full overflow-hidden border border-neutral-700/30">
                    <div 
                      className="bg-gradient-to-r from-green-600 to-green-400 h-full rounded-full transition-all duration-1000 ease-out" 
                      style={{ width: `${90 - i * 15}%` }} 
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Khung giờ phổ biến */}
        <div className="bg-neutral-900 rounded-[2.5rem] border border-neutral-800 p-10 shadow-2xl relative overflow-hidden">
          <div className="absolute bottom-0 right-0 w-64 h-64 bg-green-500/5 blur-[100px] -mr-32 -mb-32 rounded-full" />
          <h3 className="text-2xl font-black text-white mb-8 flex items-center gap-3 relative z-10 italic uppercase tracking-tighter">
            <Clock className="w-6 h-6 text-green-500" /> Khung giờ phổ biến
          </h3>
          <div className="grid grid-cols-1 gap-6 relative z-10">
            {monthlyStats.topTimeSlots.map((slot, i) => (
              <div key={i} className={`flex flex-col p-6 rounded-3xl border transition-all ${i === 0 ? 'bg-green-500/10 border-green-500/30 shadow-lg shadow-green-500/5' : 'bg-neutral-800/30 border-neutral-700/50 hover:border-neutral-600'}`}>
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-4">
                    <div className={`w-12 h-12 rounded-2xl flex items-center justify-center ${i === 0 ? 'bg-green-500 text-neutral-900' : 'bg-neutral-800 text-neutral-500'}`}>
                      <Clock className="w-6 h-6" />
                    </div>
                    <div>
                      <p className={`text-lg font-black uppercase italic tracking-tight ${i === 0 ? 'text-white' : 'text-neutral-400'}`}>{slot.label}</p>
                      <p className="text-xs font-bold text-neutral-500 uppercase tracking-widest mt-0.5">{slot.count} giao dịch thành công</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <span className={`text-2xl font-black italic ${i === 0 ? 'text-green-500' : 'text-neutral-600'}`}>
                      {monthlyStats.bookings > 0 ? Math.round((slot.count / monthlyStats.bookings) * 100) : 0}%
                    </span>
                  </div>
                </div>
                <div className="w-full bg-neutral-950/50 h-3 rounded-full overflow-hidden border border-neutral-800">
                  <div 
                    className={`h-full rounded-full transition-all duration-1000 ease-out ${i === 0 ? 'bg-gradient-to-r from-green-600 to-green-400' : 'bg-neutral-700'}`} 
                    style={{ width: `${monthlyStats.bookings > 0 ? (slot.count / monthlyStats.bookings) * 100 : 0}%` }} 
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminDashboard;
