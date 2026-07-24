import React, { createContext, useContext, useState, useCallback } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { CheckCircle, XCircle, X } from 'lucide-react'

const ToastContext = createContext(null)

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const addToast = useCallback((message, type = 'success') => {
    const id = Date.now()
    setToasts((prev) => [...prev, { id, message, type }])
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id))
    }, 4000)
  }, [])

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const success = useCallback((msg) => addToast(msg, 'success'), [addToast])
  const error = useCallback((msg) => addToast(msg, 'error'), [addToast])

  return (
    <ToastContext.Provider value={{ success, error }}>
      {children}
      <div className="fixed bottom-6 right-6 z-[100] flex flex-col gap-3 pointer-events-none">
        <AnimatePresence>
          {toasts.map((toast) => (
            <motion.div
              key={toast.id}
              initial={{ opacity: 0, y: 20, scale: 0.9 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, scale: 0.9, transition: { duration: 0.15 } }}
              className={`flex items-start gap-3 p-4 rounded-xl shadow-lg border pointer-events-auto min-w-[300px] ${
                toast.type === 'success'
                  ? 'bg-success-light/90 border-success/20 text-teal-900'
                  : 'bg-danger-light/90 border-danger/20 text-red-900'
              } backdrop-blur-md`}
            >
              <div className="mt-0.5 flex-shrink-0">
                {toast.type === 'success' ? (
                  <CheckCircle className="text-success" size={20} />
                ) : (
                  <XCircle className="text-danger" size={20} />
                )}
              </div>
              <p className="flex-1 text-sm font-medium m-0 pt-0.5">{toast.message}</p>
              <button
                onClick={() => removeToast(toast.id)}
                className="text-slate-500 hover:text-slate-800 transition-colors"
              >
                <X size={16} />
              </button>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  return useContext(ToastContext)
}
