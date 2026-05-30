/*
 * ChronoClass — Single Page Application Frontend JS
 * Copyright (c) 2026 ChronoClass. All rights reserved.
 * This software is submitted for evaluation purposes only.
 */

// Popular IANA Timezones for user selection
const POPULAR_TIMEZONES = [
    "UTC",
    "Africa/Cairo", "Africa/Johannesburg", "Africa/Lagos", "Africa/Nairobi",
    "America/Anchorage", "America/Argentina/Buenos_Aires", "America/Bogota", "America/Caracas",
    "America/Chicago", "America/Denver", "America/Halifax", "America/Los_Angeles",
    "America/Mexico_City", "America/New_York", "America/Phoenix", "America/Sao_Paulo", "America/St_Johns",
    "Asia/Bangkok", "Asia/Colombo", "Asia/Dhaka", "Asia/Dubai", "Asia/Hong_Kong",
    "Asia/Jakarta", "Asia/Jerusalem", "Asia/Kabul", "Asia/Karachi", "Asia/Kolkata",
    "Asia/Kathmandu", "Asia/Manila", "Asia/Riyadh", "Asia/Seoul", "Asia/Shanghai",
    "Asia/Singapore", "Asia/Taipei", "Asia/Tashkent", "Asia/Tokyo",
    "Australia/Adelaide", "Australia/Brisbane", "Australia/Darwin", "Australia/Melbourne", "Australia/Sydney",
    "Europe/Amsterdam", "Europe/Athens", "Europe/Berlin", "Europe/Brussels", "Europe/Istanbul",
    "Europe/Lisbon", "Europe/London", "Europe/Madrid", "Europe/Moscow", "Europe/Paris", "Europe/Rome",
    "Pacific/Auckland", "Pacific/Fiji", "Pacific/Honolulu"
];

// App State
let appState = {
    currentPortal: 'parent', // 'parent' or 'teacher'
    userId: 2,               // Default Parent ID = 2, Teacher ID = 1
    timezone: 'UTC',         // Default Timezone, updated to local on load
    parentActiveTab: 'browse' // 'browse' or 'bookings'
};

// Base URL for backend APIs
const API_BASE_URL = '/api/v1';

// On Document Load
document.addEventListener("DOMContentLoaded", () => {
    initTimezones();
    initApp();
    lucide.createIcons();
});

/* ==========================================================================
   Initialization Functions
   ========================================================================== */
function initTimezones() {
    // Detect system timezone
    const systemTz = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
    appState.timezone = systemTz;

    // Check if system timezone is in popular list, if not add it
    if (!POPULAR_TIMEZONES.includes(systemTz)) {
        POPULAR_TIMEZONES.unshift(systemTz);
    }

    // Populate Timezone dropdowns
    const selectGlobalTz = document.getElementById('select-timezone');
    const selectTeacherTz = document.getElementById('input-teacher-timezone');

    selectGlobalTz.innerHTML = '';
    selectTeacherTz.innerHTML = '';

    POPULAR_TIMEZONES.forEach(tz => {
        // Global display selector
        const optGlobal = document.createElement('option');
        optGlobal.value = tz;
        optGlobal.textContent = tz.replace('_', ' ');
        if (tz === systemTz) optGlobal.selected = true;
        selectGlobalTz.appendChild(optGlobal);

        // Teacher creation selector
        const optTeacher = document.createElement('option');
        optTeacher.value = tz;
        optTeacher.textContent = tz.replace('_', ' ');
        if (tz === systemTz) optTeacher.selected = true;
        selectTeacherTz.appendChild(optTeacher);
    });
}

function initApp() {
    // Synchronize UI inputs to initial state
    document.getElementById('input-user-id').value = appState.userId;
    document.getElementById('select-timezone').value = appState.timezone;

    // Initial fetch based on default portal
    switchPortal(appState.currentPortal);
}

/* ==========================================================================
   Core Event Handlers & State Sync
   ========================================================================== */
function switchPortal(portal) {
    appState.currentPortal = portal;

    // UI elements
    const btnParent = document.getElementById('btn-portal-parent');
    const btnTeacher = document.getElementById('btn-portal-teacher');
    const portalParent = document.getElementById('portal-parent');
    const portalTeacher = document.getElementById('portal-teacher');
    const labelUserId = document.getElementById('user-id-label');
    const inputUserId = document.getElementById('input-user-id');

    if (portal === 'parent') {
        btnParent.classList.add('active');
        btnTeacher.classList.remove('active');
        portalParent.classList.add('active');
        portalTeacher.classList.remove('active');

        labelUserId.textContent = 'Parent ID';
        // Reset to parent default if it was 1
        if (appState.userId === 1) {
            appState.userId = 2;
            inputUserId.value = 2;
        }

        switchParentTab(appState.parentActiveTab);
    } else {
        btnParent.classList.remove('active');
        btnTeacher.classList.add('active');
        portalParent.classList.remove('active');
        portalTeacher.classList.add('active');

        labelUserId.textContent = 'Teacher ID';
        // Reset to teacher default if it was 2
        if (appState.userId === 2) {
            appState.userId = 1;
            inputUserId.value = 1;
        }

        fetchTeacherOfferings();
    }

    updateStatsTimezones();
}

function switchParentTab(tab) {
    appState.parentActiveTab = tab;

    const tabBtnBrowse = document.getElementById('tab-btn-browse');
    const tabBtnBookings = document.getElementById('tab-btn-bookings');
    const tabBrowse = document.getElementById('parent-tab-browse');
    const tabBookings = document.getElementById('parent-tab-bookings');

    if (tab === 'browse') {
        tabBtnBrowse.classList.add('active');
        tabBtnBookings.classList.remove('active');
        tabBrowse.classList.add('active');
        tabBookings.classList.remove('active');

        fetchAvailableOfferings();
    } else {
        tabBtnBrowse.classList.remove('active');
        tabBtnBookings.classList.add('active');
        tabBrowse.classList.remove('active');
        tabBookings.classList.add('active');

        fetchParentBookings();
    }
}

function onUserIdChange() {
    const val = parseInt(document.getElementById('input-user-id').value);
    if (!isNaN(val) && val >= 1 && val <= 10) {
        appState.userId = val;
        showToast("Identity Updated", `Acting as ${appState.currentPortal === 'teacher' ? 'Teacher' : 'Parent'} ID ${val}`, 'info');

        // Refresh data
        if (appState.currentPortal === 'teacher') {
            fetchTeacherOfferings();
        } else {
            if (appState.parentActiveTab === 'bookings') {
                fetchParentBookings();
            } else {
                fetchAvailableOfferings();
            }
        }
    } else {
        showToast("Invalid ID Range", "ID must be between 1 and 10 for evaluation.", "warning");
        document.getElementById('input-user-id').value = appState.userId;
    }
}

function onTimezoneChange() {
    const tz = document.getElementById('select-timezone').value;
    appState.timezone = tz;
    showToast("Timezone Display Switched", `Viewing all session schedules in ${tz}`, 'info');

    updateStatsTimezones();

    // Refresh active views
    if (appState.currentPortal === 'teacher') {
        fetchTeacherOfferings();
    } else {
        if (appState.parentActiveTab === 'bookings') {
            fetchParentBookings();
        } else {
            fetchAvailableOfferings();
        }
    }
}

function updateStatsTimezones() {
    document.getElementById('stat-parent-timezone').textContent = appState.timezone;
    document.getElementById('stat-teacher-timezone').textContent = appState.timezone;
}

/* ==========================================================================
   Teacher Operations (APIs & UI Render)
   ========================================================================== */
async function handleCreateOffering(e) {
    e.preventDefault();

    const courseName = document.getElementById('input-course-name').value.trim();
    const courseDescription = document.getElementById('input-course-desc').value.trim();
    const title = document.getElementById('input-offering-title').value.trim();
    const maxStudents = parseInt(document.getElementById('input-max-students').value);
    const teacherTimezone = document.getElementById('input-teacher-timezone').value;

    if (!courseName || !title || isNaN(maxStudents) || maxStudents <= 0) {
        showToast("Validation Error", "Please fill in all required fields correctly.", "error");
        return;
    }

    const payload = { courseName, courseDescription, title, maxStudents, teacherTimezone };

    try {
        const response = await fetch(`${API_BASE_URL}/teachers/${appState.userId}/offerings`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const errData = await response.json();
            throw new Error(errData.message || "Failed to create offering.");
        }

        const data = await response.json();
        showToast("Offering Created Successfully", `"${title}" has been created.`, "success");

        // Reset form details
        document.getElementById('input-course-name').value = '';
        document.getElementById('input-course-desc').value = '';
        document.getElementById('input-offering-title').value = '';

        fetchTeacherOfferings();
    } catch (err) {
        showToast("Failed to Create Offering", err.message, "error");
    }
}

async function fetchTeacherOfferings() {
    const listContainer = document.getElementById('teacher-offerings-list');
    listContainer.innerHTML = '<div class="text-secondary text-center py-4">Loading your offerings...</div>';

    try {
        const response = await fetch(`${API_BASE_URL}/teachers/${appState.userId}/offerings`);
        if (!response.ok) throw new Error("Could not retrieve teacher offerings.");

        const offerings = await response.json();

        // Update stats
        document.getElementById('stat-teacher-offerings').textContent = offerings.length;
        const totalEnroll = offerings.reduce((acc, off) => acc + (off.enrolledCount || 0), 0);
        document.getElementById('stat-teacher-enrollments').textContent = totalEnroll;

        if (offerings.length === 0) {
            listContainer.innerHTML = `
                <div class="glass text-center py-5 text-secondary" style="border-radius: var(--radius-md); border-style: dashed;">
                    <i data-lucide="inbox" style="width: 40px; height: 40px; margin: 0 auto 12px; display: block; opacity: 0.5;"></i>
                    <p class="font-medium">No offerings created yet</p>
                    <p class="text-xs text-muted mt-1">Create one using the form on the left to get started.</p>
                </div>`;
            lucide.createIcons();
            return;
        }

        listContainer.innerHTML = '';
        offerings.forEach(off => {
            const row = document.createElement('div');
            row.className = 'teacher-offering-row';

            const sessionsCount = off.sessions ? off.sessions.length : 0;

            row.innerHTML = `
                <div class="teacher-offering-info">
                    <span class="course-category">${off.courseName}</span>
                    <h4 class="teacher-offering-title">${off.title}</h4>
                    <div class="teacher-offering-stats">
                        <span class="teacher-stat-pill"><i data-lucide="users"></i> ${off.enrolledCount} / ${off.maxStudents} enrolled</span>
                        <span class="teacher-stat-pill"><i data-lucide="calendar"></i> ${sessionsCount} sessions</span>
                        <span class="teacher-stat-pill"><i data-lucide="globe"></i> ${off.teacherTimezone}</span>
                    </div>
                    <div class="mt-2 text-xs" style="color: var(--primary); font-weight: 500; display: flex; align-items: center; gap: 4px;">
                        <i data-lucide="user-check" style="width: 13px; height: 13px;"></i>
                        <span><strong>Enrolled Parents:</strong> ${off.enrolledParentIds && off.enrolledParentIds.length > 0 ? `IDs: ${off.enrolledParentIds.join(', ')}` : 'None yet'}</span>
                    </div>
                </div>
                <div class="teacher-offering-actions">
                    <button class="btn btn-secondary btn-sm" onclick="openAddSessionsModal(${off.id}, '${off.title.replace(/'/g, "\\'")}', '${off.teacherTimezone}')">
                        <i data-lucide="calendar-plus"></i> Add Sessions
                    </button>
                    <button class="btn btn-primary btn-sm" onclick="showOfferingDetailModal(${off.id})">
                        <i data-lucide="eye"></i> Details
                    </button>
                </div>
            `;
            listContainer.appendChild(row);
        });

        lucide.createIcons();
    } catch (err) {
        listContainer.innerHTML = `<div class="text-danger py-4 text-center">Error: ${err.message}</div>`;
    }
}

/* ==========================================================================
   Add Sessions Modal Logic
   ========================================================================== */
function openAddSessionsModal(offeringId, offeringTitle, teacherTz) {
    document.getElementById('modal-input-offering-id').value = offeringId;
    document.getElementById('modal-offering-title').textContent = `Add Sessions for "${offeringTitle}"`;
    document.getElementById('modal-offering-subtitle').textContent = `Define class schedules. Enter times in the teacher's timezone (${teacherTz}).`;

    // Clear and create first row
    const container = document.getElementById('sessions-rows-container');
    container.innerHTML = '';
    addSessionRow();

    // Restore modal actions bar for adding sessions
    document.querySelector('.modal-actions-bar').innerHTML = `
        <button type="button" class="btn btn-secondary" onclick="addSessionRow()">
            <i data-lucide="plus"></i> Add Another Slot
        </button>
        <div class="modal-submit-group">
            <button type="button" class="btn btn-text" onclick="closeSessionsModal()">Cancel</button>
            <button type="submit" class="btn btn-primary">
                <i data-lucide="save"></i> Save Sessions
            </button>
        </div>
    `;

    const modal = document.getElementById('modal-add-sessions');
    modal.classList.add('active');
    lucide.createIcons();
}

function closeSessionsModal() {
    const modal = document.getElementById('modal-add-sessions');
    modal.classList.remove('active');
}

function addSessionRow() {
    const container = document.getElementById('sessions-rows-container');
    const rowId = 'session-row-' + Date.now() + '-' + Math.random().toString(36).substr(2, 4);

    const row = document.createElement('div');
    row.className = 'session-row-entry';
    row.id = rowId;

    // Set a default date for easier testing (e.g. tomorrow, 6 PM - 7 PM)
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);

    // Format YYYY-MM-DD
    const yyyy = tomorrow.getFullYear();
    const mm = String(tomorrow.getMonth() + 1).padStart(2, '0');
    const dd = String(tomorrow.getDate()).padStart(2, '0');
    const dateStr = `${yyyy}-${mm}-${dd}`;

    const defaultStart = `${dateStr}T18:00`;
    const defaultEnd = `${dateStr}T19:00`;

    row.innerHTML = `
        <button type="button" class="btn-remove-session" onclick="removeSessionRow('${rowId}')">
            <i data-lucide="trash-2"></i>
        </button>
        <div class="session-row-fields">
            <div class="form-group">
                <label>Start Date & Time</label>
                <input type="datetime-local" class="input-session-start" required value="${defaultStart}">
            </div>
            <div class="form-group">
                <label>End Date & Time</label>
                <input type="datetime-local" class="input-session-end" required value="${defaultEnd}">
            </div>
        </div>
    `;
    container.appendChild(row);
    lucide.createIcons();
}

function removeSessionRow(rowId) {
    const container = document.getElementById('sessions-rows-container');
    if (container.children.length > 1) {
        const row = document.getElementById(rowId);
        row.style.animation = 'fadeOut var(--transition-fast)';
        setTimeout(() => row.remove(), 200);
    } else {
        showToast("Validation Error", "At least one session time slot is required.", "warning");
    }
}

async function handleAddSessions(e) {
    e.preventDefault();

    const offeringId = document.getElementById('modal-input-offering-id').value;
    const startInputs = document.querySelectorAll('.input-session-start');
    const endInputs = document.querySelectorAll('.input-session-end');

    const sessions = [];
    for (let i = 0; i < startInputs.length; i++) {
        let startTime = startInputs[i].value;
        let endTime = endInputs[i].value;

        if (!startTime || !endTime) {
            showToast("Validation Error", "All start and end times must be provided.", "error");
            return;
        }

        // Ensure times format matches LocalDateTime exactly by appending seconds if needed
        if (startTime.length === 16) startTime += ':00';
        if (endTime.length === 16) endTime += ':00';

        // Simple sanity check
        if (new Date(startTime) >= new Date(endTime)) {
            showToast("Invalid Session Time", `Start time must be before end time in row ${i + 1}.`, "error");
            return;
        }

        sessions.push({ startTime, endTime });
    }

    try {
        const response = await fetch(`${API_BASE_URL}/offerings/${offeringId}/sessions`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessions })
        });

        if (!response.ok) {
            const errData = await response.json();
            throw new Error(errData.message || "Failed to add sessions.");
        }

        showToast("Sessions Scheduled", `Successfully added ${sessions.length} sessions.`, "success");
        closeSessionsModal();
        fetchTeacherOfferings();
    } catch (err) {
        showToast("Scheduling Failed", err.message, "error");
    }
}
// Show offering details (for viewing teacher offerings' sessions)
async function showOfferingDetailModal(offeringId) {
    try {
        const response = await fetch(`${API_BASE_URL}/offerings/${offeringId}?timezone=${appState.timezone}`);
        if (!response.ok) throw new Error("Could not retrieve offering details.");

        const off = await response.json();

        // Reuse modal container for display details
        const container = document.getElementById('sessions-rows-container');
        container.innerHTML = `
            <div class="glass p-4 mb-3" style="border-radius: var(--radius-md);">
                <div class="course-category">${off.courseName}</div>
                <h3 class="mt-1">${off.title}</h3>
                <p class="text-secondary mt-2 text-xs">${off.courseDescription || 'No description provided.'}</p>
                <div class="mt-4 grid-2" style="display:grid; grid-template-columns: 1fr 1fr; gap:10px; font-size:12.5px;">
                    <div><strong>Teacher Timezone:</strong> ${off.teacherTimezone}</div>
                    <div><strong>Display Timezone:</strong> ${off.displayTimezone}</div>
                    <div><strong>Max Seats:</strong> ${off.maxStudents}</div>
                    <div><strong>Enrolled Count:</strong> ${off.enrolledCount}</div>
                </div>
                <div class="mt-4 pt-3" style="border-top: 1px solid var(--border-color); font-size:12.5px;">
                    <strong>Enrolled Parent IDs:</strong>
                    <div class="mt-2" style="display:flex; flex-wrap:wrap; gap:6px;">
                        ${off.enrolledParentIds && off.enrolledParentIds.length > 0 ?
                            off.enrolledParentIds.map(pId => `<span class="teacher-stat-pill" style="margin:0;"><i data-lucide="user"></i> Parent ID ${pId}</span>`).join('') :
                            '<span class="text-muted text-xs">No parents registered yet.</span>'
                        }
                    </div>
                </div>
            </div>
            <h4>Scheduled Class Sessions</h4>
            <div class="sessions-list-small mt-2">
                ${off.sessions && off.sessions.length > 0 ?
                off.sessions.map((s, idx) => `
                        <div class="session-item-row">
                            <span class="session-number-badge">${idx + 1}</span>
                            <div class="session-times">
                                <span class="session-date">${formatDateTime(s.startTime)}</span>
                                <span class="session-time-range">${s.durationMinutes} mins duration</span>
                            </div>
                        </div>
                    `).join('') : '<div class="no-sessions-alert">No sessions scheduled yet.</div>'
            }
            </div>
        `;

        document.getElementById('modal-offering-title').textContent = `Offering Details`;
        document.getElementById('modal-offering-subtitle').textContent = `Detailed schedule details displayed in ${appState.timezone}`;

        // Hide form submit button, just show close
        document.querySelector('.modal-actions-bar').innerHTML = `
            <div></div>
            <button class="btn btn-primary" onclick="closeSessionsModal()">Close</button>
        `;

        const modal = document.getElementById('modal-add-sessions');
        modal.classList.add('active');
        lucide.createIcons();
    } catch (err) {
        showToast("Error Fetching Details", err.message, "error");
    }
}

/* ==========================================================================
   Parent Operations (APIs & UI Render)
   ========================================================================== */
async function fetchAvailableOfferings() {
    const grid = document.getElementById('available-offerings-grid');
    grid.innerHTML = '<div class="text-secondary text-center py-5" style="grid-column: 1/-1;">Loading active class offerings...</div>';

    try {
        const response = await fetch(`${API_BASE_URL}/offerings?timezone=${appState.timezone}`);
        if (!response.ok) throw new Error("Could not retrieve offerings list.");

        const offerings = await response.json();

        // Update Stats
        document.getElementById('stat-parent-available').textContent = offerings.length;

        if (offerings.length === 0) {
            grid.innerHTML = `
                <div class="glass text-center py-5 text-secondary" style="grid-column: 1/-1; border-radius: var(--radius-md); border-style: dashed; padding: 60px 20px;">
                    <i data-lucide="info" style="width: 44px; height: 44px; margin: 0 auto 12px; display: block; opacity: 0.5; color: var(--primary);"></i>
                    <p class="font-medium">No live classes are currently available</p>
                    <p class="text-xs text-muted mt-1">Teachers have not posted any active offerings yet. Switch to Teacher Portal to create one!</p>
                </div>`;
            lucide.createIcons();
            return;
        }

        grid.innerHTML = '';
        offerings.forEach(off => {
            const card = document.createElement('div');
            card.className = 'offering-card';

            const percentFilled = Math.min(100, Math.round((off.enrolledCount / off.maxStudents) * 100));
            const availableSpots = off.availableSpots !== undefined ? off.availableSpots : (off.maxStudents - off.enrolledCount);

            const isFull = availableSpots <= 0;

            let btnHtml = '';
            if (isFull) {
                btnHtml = `<button class="btn btn-secondary btn-block" disabled><i data-lucide="user-x"></i> Sold Out</button>`;
            } else {
                btnHtml = `
                    <button class="btn btn-primary btn-block" onclick="bookOffering(${off.id}, '${off.title.replace(/'/g, "\\\'")}')">
                        <i data-lucide="ticket"></i> Book Offering
                    </button>`;
            }

            let sessionsHtml = '';
            if (off.sessions && off.sessions.length > 0) {
                sessionsHtml = off.sessions.map((s, idx) => `
                    <li class="session-item-row">
                        <span class="session-number-badge">${idx + 1}</span>
                        <div class="session-times">
                            <span class="session-date">${formatDateTime(s.startTime)}</span>
                            <span class="session-time-range">${s.durationMinutes} mins duration</span>
                        </div>
                    </li>
                `).join('');
                sessionsHtml = `<ul class="sessions-list-small">${sessionsHtml}</ul>`;
            } else {
                sessionsHtml = `<div class="no-sessions-alert"><i data-lucide="alert-circle"></i> No sessions scheduled yet.</div>`;
            }

            card.innerHTML = `
                <div class="offering-card-header">
                    <span class="course-category">${off.courseName}</span>
                    <span class="offering-meta-badge active">${off.status || 'Active'}</span>
                    <h3 class="course-title">${off.title}</h3>
                    <div class="mt-2" style="display: flex; gap: 8px;">
                        <span class="teacher-stat-pill" style="margin: 0;"><i data-lucide="presentation"></i> Teacher ID: ${off.teacherId}</span>
                    </div>
                    <p class="course-desc">${off.courseDescription || 'No course description available. Learn with peers globally.'}</p>
                </div>
                
                <div class="offering-capacity-container">
                    <div class="capacity-label-row">
                        <span>Class Capacity</span>
                        <span class="font-medium">${off.enrolledCount} / ${off.maxStudents} Booked</span>
                    </div>
                    <div class="capacity-progress-bg">
                        <div class="capacity-progress-fill ${isFull ? 'full' : ''}" style="width: ${percentFilled}%"></div>
                    </div>
                </div>
                
                <div class="sessions-schedule-area">
                    <span class="sessions-title"><i data-lucide="calendar"></i> Session Times (in ${appState.timezone})</span>
                    ${sessionsHtml}
                </div>
                
                <div class="offering-card-footer">
                    ${btnHtml}
                </div>
            `;

            grid.appendChild(card);
        });

        lucide.createIcons();
    } catch (err) {
        grid.innerHTML = `<div class="text-danger text-center py-5" style="grid-column: 1/-1;">Error: ${err.message}</div>`;
    }
}



async function bookOffering(offeringId, offeringTitle) {
    const payload = {
        offeringId: offeringId,
        parentTimezone: appState.timezone
    };

    try {
        const response = await fetch(`${API_BASE_URL}/parents/${appState.userId}/bookings`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const errData = await response.json();
            throw new Error(errData.message || "Booking failed.");
        }

        const booking = await response.json();
        showToast("Booking Successful!", `You are registered for "${offeringTitle}".`, "success");

        // Refresh available listings and switch to bookings tab to see schedule
        fetchAvailableOfferings();
        setTimeout(() => switchParentTab('bookings'), 600);
    } catch (err) {
        showToast("Booking Conflict Detected", err.message, "error");
    }
}

async function fetchParentBookings() {
    const grid = document.getElementById('parent-bookings-grid');
    grid.innerHTML = '<div class="text-secondary text-center py-5" style="grid-column: 1/-1;">Loading your booked schedule...</div>';

    try {
        const response = await fetch(`${API_BASE_URL}/parents/${appState.userId}/bookings?timezone=${appState.timezone}`);
        if (!response.ok) throw new Error("Could not retrieve bookings.");

        const bookings = await response.json();

        // Update Stats
        document.getElementById('stat-parent-bookings').textContent = bookings.length;

        if (bookings.length === 0) {
            grid.innerHTML = `
                <div class="glass text-center py-5 text-secondary" style="grid-column: 1/-1; border-radius: var(--radius-md); border-style: dashed; padding: 60px 20px;">
                    <i data-lucide="calendar-days" style="width: 44px; height: 44px; margin: 0 auto 12px; display: block; opacity: 0.5; color: var(--primary);"></i>
                    <p class="font-medium">No active bookings found for Parent ID ${appState.userId}</p>
                    <p class="text-xs text-muted mt-1">Browse available offerings and secure your spots today!</p>
                </div>`;
            lucide.createIcons();
            return;
        }

        grid.innerHTML = '';
        bookings.forEach(b => {
            const off = b.offering;
            if (!off) return;

            const card = document.createElement('div');
            card.className = 'offering-card';

            let sessionsHtml = '';
            if (off.sessions && off.sessions.length > 0) {
                sessionsHtml = off.sessions.map((s, idx) => `
                    <li class="session-item-row" style="border-color: rgba(0, 200, 83, 0.3); background: rgba(0, 200, 83, 0.03);">
                        <div class="session-checkbox" style="background: rgba(0, 200, 83, 1); border-color: rgba(0, 200, 83, 1); color: white;">
                            <i data-lucide="check"></i>
                        </div>
                        <span class="session-number-badge">${idx + 1}</span>
                        <div class="session-times">
                            <span class="session-date">${formatDateTime(s.startTime)}</span>
                            <span class="session-time-range">${s.durationMinutes} mins duration</span>
                        </div>
                        <div style="margin-left: auto; font-size: 11px; color: rgba(0, 200, 83, 1); font-weight: 600;">
                            Confirmed
                        </div>
                    </li>
                `).join('');
                sessionsHtml = `<ul class="sessions-list-small">${sessionsHtml}</ul>`;
            } else {
                sessionsHtml = `<div class="no-sessions-alert">No sessions scheduled for this class.</div>`;
            }

            card.innerHTML = `
                <div class="offering-card-header">
                    <span class="course-category">${off.courseName}</span>
                    <span class="offering-meta-badge booked">Booked</span>
                    <h3 class="course-title">${off.title}</h3>
                    <div class="mt-2" style="display: flex; gap: 8px;">
                        <span class="teacher-stat-pill" style="margin: 0;"><i data-lucide="presentation"></i> Teacher ID: ${off.teacherId}</span>
                    </div>
                    <p class="course-desc">${off.courseDescription || 'No description provided.'}</p>
                    <p class="text-xs text-muted mt-2">Booked at: ${formatDateTime(b.bookedAt)}</p>
                </div>
                
                <div class="sessions-schedule-area">
                    <span class="sessions-title"><i data-lucide="calendar-check"></i> Session Times (in ${appState.timezone})</span>
                    <ul class="sessions-list-small">
                        ${sessionsHtml}
                    </ul>
                </div>
                
                <div class="offering-card-footer">
                    <div class="glass p-3 text-center text-xs text-primary" style="border-radius: var(--radius-sm); border-color: rgba(0, 0, 255, 0.15); font-weight:600;">
                        <i data-lucide="check-circle" style="width: 14px; height: 14px; display:inline-block; vertical-align:middle; margin-right:4px;"></i> Confirmed Seat
                    </div>
                </div>
            `;
            grid.appendChild(card);
        });

        lucide.createIcons();
    } catch (err) {
        grid.innerHTML = `<div class="text-danger text-center py-5" style="grid-column: 1/-1;">Error: ${err.message}</div>`;
    }
}

/* ==========================================================================
   Helper Utilities
   ========================================================================== */
function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '-';
    try {
        const date = new Date(dateTimeStr);
        if (isNaN(date.getTime())) return dateTimeStr;

        const optionsDate = { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' };
        const optionsTime = { hour: '2-digit', minute: '2-digit', hour12: true };

        return date.toLocaleDateString('en-US', optionsDate) + ' • ' + date.toLocaleTimeString('en-US', optionsTime);
    } catch (e) {
        return dateTimeStr;
    }
}

// Toast alerts engine
function showToast(title, desc, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    let iconName = 'info';
    if (type === 'success') iconName = 'check-circle';
    if (type === 'error') iconName = 'alert-triangle';
    if (type === 'warning') iconName = 'alert-circle';

    toast.innerHTML = `
        <i data-lucide="${iconName}" class="toast-icon"></i>
        <div class="toast-content">
            <h4 class="toast-title">${title}</h4>
            <p class="toast-desc">${desc}</p>
        </div>
        <span class="toast-close" onclick="this.parentElement.remove()">&times;</span>
    `;

    container.appendChild(toast);
    lucide.createIcons();

    // Automatically trigger slideOut and remove toast
    setTimeout(() => {
        if (toast.parentElement) {
            toast.style.animation = 'slideOutRight 0.3s cubic-bezier(0.4, 0, 0.2, 1) forwards';
            setTimeout(() => toast.remove(), 300);
        }
    }, 4500);
}
