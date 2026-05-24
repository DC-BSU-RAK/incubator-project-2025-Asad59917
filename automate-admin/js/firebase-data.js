const firebaseConfig = {
    apiKey: "AIzaSyBwdMkBO1Sok5sdwMwt1a6kp4_X-vNwb5w",
    authDomain: "automate-carservice.firebaseapp.com",
    projectId: "automate-carservice",
    storageBucket: "automate-carservice.firebasestorage.app",
    messagingSenderId: "413791610104",
    appId: "1:413791610104:web:2bce74a3bec44444fb470d"
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);
const db = firebase.firestore();


// USERS
async function fetchUsers() {
    try {
        const snapshot = await db.collection('users').get();
        const users = [];

        for (const doc of snapshot.docs) {
            const data = doc.data();

            // Calculate user stats from their bookings
            const bookingsSnap = await db.collection('bookings')
                .where('userId', '==', doc.id)
                .get();

            let totalBookings = bookingsSnap.size;
            let totalSpent = 0;
            bookingsSnap.docs.forEach(b => {
                if (b.data().status === 'COMPLETED') {
                    totalSpent += b.data().price || 0;
                }
            });

            users.push({
                id: doc.id,
                name: data.name || 'Unknown',
                email: data.email || '',
                phone: data.phone || '',
                isPro: data.isPremium === true,
                joinDate: data.createdAt
                    ? new Date(data.createdAt).toISOString().split('T')[0]
                    : '2026-01-01',
                totalBookings: totalBookings,
                totalSpent: totalSpent
            });
        }
        return users;
    } catch (error) {
        console.error('Error fetching users:', error);
        return [];
    }
}


// BOOKINGS

async function fetchBookings() {
    try {
        const snapshot = await db.collection('bookings').get();
        const bookings = snapshot.docs.map(doc => {
            const data = doc.data();
            return {
                id: data.id || doc.id,
                docId: doc.id,  
                userId: data.userId || '',
                userName: data.userName || 'Unknown',
                userEmail: data.userEmail || '',
                serviceId: data.serviceId || '',
                serviceName: data.serviceName || '',
                vehicleBrand: data.vehicleBrand || '',
                vehicleModel: data.vehicleModel || '',
                plateNumber: data.plateNumber || '',
                date: data.bookingDate || '',
                time: data.time || '',
                location: data.location || '',
                originalPrice: data.originalPrice || data.price || 0,
                price: data.price || 0,
                isProDiscount: data.isProDiscount === true,
                status: data.status || 'PENDING',
                createdAt: data.createdAt
                    ? new Date(data.createdAt).toISOString().split('T')[0]
                    : ''
            };
        });

        bookings.sort((a, b) => {
            return new Date(b.createdAt) - new Date(a.createdAt);
        });

        return bookings;
    } catch (error) {
        console.error('Error fetching bookings:', error);
        return [];
    }
}

async function updateBookingStatus(bookingId, newStatus) {
    try {
        await db.collection('bookings').doc(bookingId).update({
            status: newStatus
        });
        return true;
    } catch (error) {
        console.error('Error updating booking:', error);
        return false;
    }
}


// VEHICLES

async function fetchVehicles() {
    try {
        const snapshot = await db.collection('vehicles').get();

        // Look up user names for each vehicle's owner
        const usersMap = {};
        const usersSnap = await db.collection('users').get();
        usersSnap.docs.forEach(u => {
            usersMap[u.id] = u.data().name || 'Unknown';
        });

        return snapshot.docs.map(doc => {
            const data = doc.data();
            return {
                id: doc.id,
                userId: data.userId || '',
                userName: usersMap[data.userId] || 'Unknown',
                brand: data.brand || '',
                model: data.model || '',
                color: data.color || '',
                chassisNumber: data.chassisNumber || '',
                plateNumber: data.plateNumber || '',
                type: data.type || 'Car'
            };
        });
    } catch (error) {
        console.error('Error fetching vehicles:', error);
        return [];
    }
}


// SERVICES

async function fetchServices() {
    try {
        const snapshot = await db.collection('services').get();
        if (snapshot.empty) {
            // No services in Firebase yet - return defaults
            return getDefaultServices();
        }
        return snapshot.docs.map(doc => ({
            id: doc.id,
            ...doc.data()
        }));
    } catch (error) {
        console.error('Error fetching services:', error);
        return getDefaultServices();
    }
}

function getDefaultServices() {
    return [
        { id: "S001", name: "Engine Oil Change", basePrice: 100, icon: "🛢️", description: "Premium engine oil change service", isActive: true },
        { id: "S002", name: "Car Towing", basePrice: 150, icon: "🚛", description: "24/7 emergency towing service", isActive: true },
        { id: "S003", name: "Brake Service", basePrice: 200, icon: "🛞", description: "Complete brake system inspection & repair", isActive: true },
        { id: "S004", name: "Car Wash", basePrice: 50, icon: "🚿", description: "Premium car wash with wax", isActive: true },
        { id: "S005", name: "Fuel Up", basePrice: 100, icon: "⛽", description: "Emergency fuel delivery", isActive: true },
        { id: "S006", name: "Tire Change", basePrice: 120, icon: "⚙️", description: "Tire change and balancing service", isActive: true },
        { id: "S007", name: "Battery Change", basePrice: 180, icon: "🔋", description: "Car battery replacement service", isActive: true },
        { id: "S008", name: "Service Contract", basePrice: 500, icon: "📋", description: "Annual service maintenance contract", isActive: true }
    ];
}

async function saveServiceUpdate(serviceId, data) {
    try {
        await db.collection('services').doc(serviceId).set(data, { merge: true });
        return true;
    } catch (error) {
        console.error('Error saving service:', error);
        return false;
    }
}

//
// PRO SUBSCRIPTIONS
//
async function cancelUserPro(userId) {
    try {
        await db.collection('users').doc(userId).update({
            isPremium: false,
            premiumPlan: '',
            premiumCancelledAt: Date.now()
        });
        return true;
    } catch (error) {
        console.error('Error cancelling pro:', error);
        return false;
    }
}

//
// NOTIFICATIONS
//
async function fetchNotifications() {
    try {
        const snapshot = await db.collection('notifications')
            .orderBy('createdAt', 'desc')
            .limit(50)
            .get();
        return snapshot.docs.map(doc => ({
            id: doc.id,
            ...doc.data()
        }));
    } catch (error) {
        console.error('Error fetching notifications:', error);
        return [];
    }
}

async function saveNotification(notif) {
    try {
        await db.collection('notifications').add({
            ...notif,
            createdAt: Date.now()
        });
        return true;
    } catch (error) {
        console.error('Error saving notification:', error);
        return false;
    }
}

//
// STATS (Dashboard)
//
async function computeStats() {
    const [users, bookings] = await Promise.all([
        fetchUsers(),
        fetchBookings()
    ]);

    const totalRevenue = bookings
        .filter(b => b.status === 'COMPLETED')
        .reduce((sum, b) => sum + (b.price || 0), 0);

    const today = new Date().toISOString().split('T')[0];
    const todayBookings = bookings.filter(b => b.createdAt === today).length;
    const proUsers = users.filter(u => u.isPro).length;

    return {
        totalRevenue,
        totalBookings: bookings.length,
        totalUsers: users.length,
        proUsers,
        conversionRate: users.length > 0
            ? ((proUsers / users.length) * 100).toFixed(1)
            : '0',
        todayBookings,
        pendingBookings: bookings.filter(b => b.status === 'PENDING').length,
        confirmedBookings: bookings.filter(b => b.status === 'CONFIRMED').length,
        completedBookings: bookings.filter(b => b.status === 'COMPLETED').length,
        cancelledBookings: bookings.filter(b => b.status === 'CANCELLED').length
    };
}

//
// HELPER FUNCTIONS (same as before)
//
function formatAED(amount) {
    return `AED ${(amount || 0).toLocaleString()}`;
}

function formatDate(dateStr) {
    if (!dateStr) return '-';
    try {
        const date = new Date(dateStr);
        if (isNaN(date.getTime())) return dateStr;
        return date.toLocaleDateString('en-US', {
            day: 'numeric',
            month: 'short',
            year: 'numeric'
        });
    } catch {
        return dateStr;
    }
}