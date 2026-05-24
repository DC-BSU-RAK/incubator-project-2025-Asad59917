// Check if admin is logged in
function checkAuth() {
    const isLoggedIn = localStorage.getItem('admin_logged_in') === 'true';
    const currentPage = window.location.pathname.split('/').pop();

    if (!isLoggedIn && currentPage !== 'index.html' && currentPage !== '') {
        window.location.href = 'index.html';
    }
}

// Logout
function logout() {
    if (confirm('Are you sure you want to logout?')) {
        localStorage.removeItem('admin_logged_in');
        window.location.href = 'index.html';
    }
}

// Build sidebar
function buildSidebar(activePage) {
    const menuItems = [
        { id: 'dashboard', icon: '📊', label: 'Dashboard', file: 'dashboard.html' },
        { id: 'bookings', icon: '📅', label: 'Bookings', file: 'bookings.html' },
        { id: 'users', icon: '👥', label: 'Users', file: 'users.html' },
        { id: 'services', icon: '🔧', label: 'Services', file: 'services.html' },
        { id: 'pro-members', icon: '⭐', label: 'Pro Members', file: 'pro-members.html' },
        { id: 'vehicles', icon: '🚗', label: 'Vehicles', file: 'vehicles.html' },
        { id: 'notifications', icon: '🔔', label: 'Notifications', file: 'notifications.html' },
        { id: 'reports', icon: '📈', label: 'Reports', file: 'reports.html' },
    ];

    const html = `
        <div class="sidebar-header">
            <div class="logo-icon">AM</div>
            <div>
                <div class="logo-text">AutoMate</div>
                <div class="logo-subtitle">Admin Panel</div>
            </div>
        </div>
        <ul class="nav-menu">
            ${menuItems.map(item => `
                <li class="nav-item">
                    <a href="${item.file}" class="nav-link ${activePage === item.id ? 'active' : ''}">
                        <span class="nav-icon">${item.icon}</span>
                        <span>${item.label}</span>
                    </a>
                </li>
            `).join('')}
        </ul>
        <div class="sidebar-footer">
            <button class="logout-btn" onclick="logout()">🚪 Logout</button>
        </div>
    `;
    return html;
}

// Build top user badge
function buildUserBadge() {
    return `
        <div class="user-badge">
            <div class="user-avatar">A</div>
            <div>
                <div class="user-name">Admin</div>
            </div>
        </div>
    `;
}

// Show toast notification
function showToast(message, isError = false) {
    const existing = document.querySelector('.toast');
    if (existing) existing.remove();

    const toast = document.createElement('div');
    toast.className = `toast ${isError ? 'error' : ''}`;
    toast.textContent = message;
    document.body.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// Get status badge HTML
function getStatusBadge(status) {
    const map = {
        'CONFIRMED': '<span class="badge badge-success">CONFIRMED</span>',
        'PENDING': '<span class="badge badge-warning">PENDING</span>',
        'COMPLETED': '<span class="badge badge-info">COMPLETED</span>',
        'CANCELLED': '<span class="badge badge-danger">CANCELLED</span>'
    };
    return map[status] || `<span class="badge badge-gray">${status}</span>`;
}
