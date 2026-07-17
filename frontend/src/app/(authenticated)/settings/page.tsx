'use client';

import { useQuery, useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { authApi } from '@/lib/api';
import { useAuth } from '@/context/AuthContext';
import { Spinner } from '@/components/ui/Spinner';
import { Alert } from '@/components/ui/Alert';
import { User, Lock, LogOut, Shield, Calendar, Mail } from 'lucide-react';

export default function SettingsPage() {
  const { logout } = useAuth();
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const { data: profile, isLoading, isError } = useQuery({
    queryKey: ['profile'],
    queryFn: authApi.me,
  });

  const { register, handleSubmit, reset, watch, formState: { errors } } = useForm({
    defaultValues: {
      oldPassword: '',
      newPassword: '',
      confirmPassword: '',
    },
  });

  const changePasswordMutation = useMutation({
    mutationFn: (data: any) => authApi.changePassword({
      oldPassword: data.oldPassword,
      newPassword: data.newPassword,
    }),
    onSuccess: () => {
      setSuccessMsg('Password changed successfully.');
      reset();
      setTimeout(() => setSuccessMsg(null), 5000);
    },
  });

  const onSubmit = (data: any) => {
    changePasswordMutation.mutate(data);
  };

  const newPassword = watch('newPassword');

  if (isLoading) {
    return (
      <div className="flex justify-center items-center py-20 min-h-screen bg-surface-primary">
        <Spinner size="lg" />
      </div>
    );
  }

  if (isError || !profile) {
    return (
      <div className="p-6 min-h-screen bg-surface-primary">
        <Alert type="error" title="Error Loading Settings">
          Could not retrieve profile settings. Please try again.
        </Alert>
      </div>
    );
  }

  return (
    <div className="animate-fade-in min-h-screen bg-surface-primary p-6 space-y-6">
      {/* Title */}
      <div className="flex items-center gap-3">
        <User className="w-8 h-8 text-brand-400" />
        <h1 className="text-3xl font-bold text-slate-100 font-display">Account Settings</h1>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left: Profile Info Card */}
        <div className="lg:col-span-1 space-y-6">
          <div className="card p-6 border border-surface-border/60 bg-surface-secondary flex flex-col items-center text-center shadow-xl">
            <div className="w-20 h-20 bg-brand-500/10 border border-brand-500/25 rounded-full flex items-center justify-center text-brand-400 mb-4">
              <User className="w-10 h-10" />
            </div>
            <h2 className="text-xl font-bold text-slate-100 font-display">{profile.name}</h2>
            <p className="text-xs font-semibold text-slate-400 bg-surface-tertiary border border-surface-border px-2 py-0.5 rounded mt-1.5 uppercase tracking-wide">
              {profile.role}
            </p>

            <div className="w-full border-t border-surface-border/60 my-6" />

            {/* Profile fields */}
            <div className="w-full space-y-4 text-left text-sm">
              <div className="flex items-center gap-3 text-slate-300">
                <Mail className="w-4 h-4 text-slate-500" />
                <div className="truncate">
                  <p className="text-[10px] text-slate-500 font-semibold uppercase tracking-wider">Email Address</p>
                  <p className="font-medium truncate">{profile.email}</p>
                </div>
              </div>

              <div className="flex items-center gap-3 text-slate-300">
                <Shield className="w-4 h-4 text-slate-500" />
                <div>
                  <p className="text-[10px] text-slate-500 font-semibold uppercase tracking-wider">Account ID</p>
                  <p className="font-mono text-xs">{profile.id}</p>
                </div>
              </div>

              <div className="flex items-center gap-3 text-slate-300">
                <Calendar className="w-4 h-4 text-slate-500" />
                <div>
                  <p className="text-[10px] text-slate-500 font-semibold uppercase tracking-wider">Registered Since</p>
                  <p className="font-medium">{new Date(profile.createdAt).toLocaleDateString()}</p>
                </div>
              </div>
            </div>

            <div className="w-full border-t border-surface-border/60 my-6" />

            <button
              onClick={logout}
              className="btn w-full bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/25 flex items-center justify-center gap-2 py-2.5 rounded font-bold text-sm transition-all"
            >
              <LogOut className="w-4 h-4" />
              Sign Out
            </button>
          </div>
        </div>

        {/* Right: Change Password Card */}
        <div className="lg:col-span-2">
          <div className="card p-6 border border-surface-border/60 bg-surface-secondary shadow-xl">
            <h2 className="text-lg font-bold text-slate-100 mb-4 font-display flex items-center gap-2">
              <Lock className="w-4 h-4 text-slate-400" />
              Change Password
            </h2>

            {successMsg && (
              <div className="mb-4">
                <Alert type="success" title="Success">
                  {successMsg}
                </Alert>
              </div>
            )}

            {changePasswordMutation.isError && (
              <div className="mb-4">
                <Alert type="error" title="Password Change Failed">
                  {changePasswordMutation.error instanceof Error ? changePasswordMutation.error.message : 'Unknown error occurred'}
                </Alert>
              </div>
            )}

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              {/* Old Password */}
              <div>
                <label className="block text-sm font-semibold text-slate-400 mb-2">Current Password</label>
                <input
                  type="password"
                  {...register('oldPassword', { required: 'Current password is required' })}
                  className={`input w-full ${errors.oldPassword ? 'input-error' : ''}`}
                  placeholder="••••••••"
                />
                {errors.oldPassword && (
                  <p className="text-red-400 text-xs mt-1">{errors.oldPassword.message}</p>
                )}
              </div>

              {/* New Password */}
              <div>
                <label className="block text-sm font-semibold text-slate-400 mb-2">New Password</label>
                <input
                  type="password"
                  {...register('newPassword', {
                    required: 'New password is required',
                    minLength: { value: 8, message: 'New password must be at least 8 characters' }
                  })}
                  className={`input w-full ${errors.newPassword ? 'input-error' : ''}`}
                  placeholder="••••••••"
                />
                {errors.newPassword && (
                  <p className="text-red-400 text-xs mt-1">{errors.newPassword.message}</p>
                )}
              </div>

              {/* Confirm Password */}
              <div>
                <label className="block text-sm font-semibold text-slate-400 mb-2">Confirm New Password</label>
                <input
                  type="password"
                  {...register('confirmPassword', {
                    required: 'Please confirm your new password',
                    validate: (val) => val === newPassword || 'Passwords do not match'
                  })}
                  className={`input w-full ${errors.confirmPassword ? 'input-error' : ''}`}
                  placeholder="••••••••"
                />
                {errors.confirmPassword && (
                  <p className="text-red-400 text-xs mt-1">{errors.confirmPassword.message}</p>
                )}
              </div>

              {/* Submit button */}
              <button
                type="submit"
                disabled={changePasswordMutation.isPending}
                className="btn bg-brand-500 hover:bg-brand-600 text-white font-bold text-sm py-3 px-6 rounded shadow-lg shadow-brand-500/10 flex items-center justify-center gap-2 disabled:opacity-50"
              >
                {changePasswordMutation.isPending ? (
                  <>
                    <Spinner size="sm" />
                    Saving...
                  </>
                ) : (
                  'Update Password'
                )}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
