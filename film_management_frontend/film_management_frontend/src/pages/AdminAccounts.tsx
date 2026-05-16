import React, { useEffect, useState } from 'react';
import { User, Shield, ShieldAlert, Search, Loader2, Mail, Phone, MapPin, X, Lock, Unlock } from 'lucide-react';
import { authService } from '../services/auth.service';
import { type Account } from '../types';
import { useToast } from '../components/Toast';

const AdminAccounts: React.FC = () => {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const { showToast } = useToast();

  useEffect(() => {
    fetchAccounts();
  }, []);

  const fetchAccounts = async () => {
    try {
      setIsLoading(true);
      const data = await authService.getAllAccounts();
      setAccounts(data);
    } catch (err: any) {
      showToast('Không thể tải danh sách tài khoản.', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  const handleToggleStatus = async (account: Account) => {
    if (!account.idAccount) return;
    
    const actionText = account.active ? 'vô hiệu hóa' : 'kích hoạt';
    
    if (window.confirm(`Bạn có chắc chắn muốn ${actionText} tài khoản này?`)) {
      try {
        await authService.toggleActive(account.idAccount);
        showToast(`Đã ${actionText} tài khoản thành công!`, 'success');
        fetchAccounts();
      } catch (err: any) {
        showToast(`Thao tác thất bại. Vui lòng thử lại.`, 'error');
      }
    }
  };

  const filteredAccounts = accounts.filter(acc => 
    `${acc.firstName} ${acc.lastName}`.toLowerCase().includes(searchTerm.toLowerCase()) ||
    acc.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
    acc.phone.includes(searchTerm)
  );

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh]">
        <Loader2 className="w-12 h-12 text-green-500 animate-spin" />
        <p className="mt-4 text-neutral-300 font-medium">Đang tải danh sách tài khoản...</p>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-white">Quản lý tài khoản</h1>
        <p className="text-neutral-300">Quản lý danh sách người dùng và trạng thái hoạt động</p>
      </div>

      <div className="bg-neutral-900 rounded-2xl border border-neutral-800 p-6 shadow-2xl">
        <div className="relative mb-6">
          <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none text-neutral-400">
            <Search className="w-5 h-5" />
          </div>
          <input
            type="text"
            placeholder="Tìm theo tên, email hoặc số điện thoại..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="block w-full pl-10 pr-3 py-3 bg-neutral-800 border border-neutral-700 rounded-xl focus:ring-2 focus:ring-green-500 focus:border-transparent text-white placeholder-neutral-500 outline-none transition-all"
          />
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-neutral-800 text-neutral-300 text-sm uppercase tracking-wider">
                <th className="px-4 py-4 font-semibold">Người dùng</th>
                <th className="px-4 py-4 font-semibold">Liên hệ</th>
                <th className="px-4 py-4 font-semibold">Trạng thái</th>
                <th className="px-4 py-4 font-semibold">Quyền hạn</th>
                <th className="px-4 py-4 font-semibold text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-800">
              {filteredAccounts.map((acc) => (
                <tr 
                  key={acc.idAccount} 
                  className={`transition-colors group ${
                    acc.active 
                      ? 'hover:bg-neutral-800/40' 
                      : 'bg-red-500/[0.03] hover:bg-red-500/[0.06]'
                  }`}
                >
                  <td className="px-4 py-4">
                    <div className="flex items-center gap-3">
                      <div className={`w-10 h-10 rounded-full bg-neutral-800 flex items-center justify-center border ${
                        acc.active 
                          ? 'border-green-500/30 text-green-500' 
                          : 'border-red-500/30 text-red-500'
                      }`}>
                        <User className="w-5 h-5" />
                      </div>
                      <div>
                        <p className={`font-bold transition-colors ${
                          acc.active ? 'text-white group-hover:text-green-400' : 'text-neutral-300'
                        }`}>
                          {acc.lastName} {acc.firstName}
                        </p>
                        <p className="text-[11px] text-neutral-400 font-mono uppercase tracking-tighter">
                          ID: {acc.idAccount}
                        </p>
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-4">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2 text-sm text-neutral-200">
                        <Mail className="w-3.5 h-3.5 text-neutral-500" /> {acc.email}
                      </div>
                      <div className="flex items-center gap-2 text-sm text-neutral-200">
                        <Phone className="w-3.5 h-3.5 text-neutral-500" /> {acc.phone}
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-4">
                    <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-bold uppercase tracking-wider ${
                      acc.active 
                        ? 'bg-green-500/10 text-green-500 border border-green-500/20' 
                        : 'bg-red-500/10 text-red-500 border border-red-500/20'
                    }`}>
                      {acc.active ? 'Đang hoạt động' : 'Đã khóa'}
                    </span>
                  </td>
                  <td className="px-4 py-4">
                    <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider border ${
                      acc.role === 'ADMIN' 
                        ? 'bg-orange-500/10 border-orange-500/20 text-orange-500' 
                        : 'bg-blue-500/10 border-blue-500/20 text-blue-500'
                    }`}>
                      {acc.role === 'ADMIN' ? <ShieldAlert className="w-3.5 h-3.5" /> : <Shield className="w-3.5 h-3.5" />}
                      {acc.role}
                    </span>
                  </td>
                  <td className="px-4 py-4 text-right">
                    <div className="flex justify-end gap-2">
                      <button 
                        onClick={() => handleToggleStatus(acc)}
                        className={`p-2 rounded-lg transition-all ${
                          acc.active 
                            ? 'bg-green-500/10 hover:bg-green-500/20 text-green-500' 
                            : 'bg-red-500/10 hover:bg-red-500/20 text-red-500'
                        }`}
                        title={acc.active ? 'Vô hiệu hóa tài khoản' : 'Kích hoạt tài khoản'}
                      >
                        {acc.active ? <Unlock className="w-5 h-5" /> : <Lock className="w-5 h-5" />}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default AdminAccounts;
