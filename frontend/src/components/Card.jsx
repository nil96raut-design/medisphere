import React from 'react'
import { twMerge } from 'tailwind-merge'

export function Card({ children, className = '', padding = 'p-6', onClick = null }) {
  const isClickable = !!onClick
  
  return (
    <div
      onClick={onClick}
      className={twMerge(
        'bg-surface rounded-xl border border-slate-200 shadow-sm transition-all duration-200',
        padding,
        isClickable ? 'cursor-pointer hover:shadow-md hover:border-primary/30' : '',
        className
      )}
    >
      {children}
    </div>
  )
}
