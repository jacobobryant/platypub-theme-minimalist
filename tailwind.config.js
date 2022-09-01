module.exports = {
  content: [
    'public/**/*.html',
  ],
  theme: {
    extend: {
      colors: {
        'bs-blue': '#007bff',
      },
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
  ],
}
