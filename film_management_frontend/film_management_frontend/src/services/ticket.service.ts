import api from './api';
import { type BookingResponseDTO, type BookingRequest, type Bill } from '../types';

export const bookingService = {

  getBookedSeats: async (idShowtime: string) => {
    const response = await api.get<string[]>(`/bookings/seats/${idShowtime}`);
    return response.data;
  },

  bookTickets: async (bookingRequest: BookingRequest) => {
    const response = await api.post<BookingResponseDTO>('/bookings', bookingRequest, {
      headers: { 'Content-Type': 'application/json' }
    });
    return response.data;
  },

  getBillDetail: async (idBill: string) => {
    const response = await api.get<BookingResponseDTO>(`/tickets/bills/${idBill}`);
    return response.data;
  },

  getHistory: async () => {
    const response = await api.get<Bill[]>(`/tickets/history`);
    return response.data;
  },

  cancelBooking: async (idBill: string) => {
    const response = await api.delete<string>(`/tickets/cancel/${idBill}`);
    return response.data;
  },

  getAllBills: async () => {
    const response = await api.get<Bill[]>('/tickets/admin/bills');
    return response.data;
  },
};
