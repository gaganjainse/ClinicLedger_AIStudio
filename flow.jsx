import React, { useState } from 'react';

// Village Clinic Ledger - Project Flow Dashboard Component
export default function ProjectFlow() {
  const [activeTab, setActiveTab] = useState('phases');

  const phases = [
    { id: 0, name: 'Foundation', status: 'Completed', desc: 'Android project, Room DB, Material 3, Hindi+English, 4 entities (Patient, Village, Alias, Transaction)' },
    { id: 1, name: 'Core Ledger', status: 'Completed', desc: 'Patient CRUD, transactions, running balance, search, village dropdown, audit trail (no deletes)' },
    { id: 2, name: 'Backup & Trust', status: 'Completed', desc: 'JSON export/import, auto daily backup via WorkManager, version validation' },
    { id: 3, name: 'Family Accounts', status: 'Completed', desc: 'FamilyGroup entity, familyGroupId on Patient, family display on detail screen' },
    { id: 4, name: 'Voice Search', status: 'Completed', desc: 'Mic icon on search bar, SpeechRecognizer, RECORD_AUDIO permission, Hindi language preference' },
    { id: 5, name: 'Voice Query → Balance', status: 'Completed', desc: 'Speaking a name shows balance dialog + "Open" button' },
    { id: 6, name: 'Analytics', status: 'Completed', desc: 'Today/week/month summaries, top patients, village breakdown' },
    { id: 7, name: 'Auto Backup', status: 'Completed', desc: 'Daily WorkManager backup, 30-day cleanup, status in Backup UI' },
    { id: 8, name: 'Voice Conversation Engine', status: 'Completed', desc: 'HindiNumberConverter, VoiceIntentParser, VoiceTtsManager, VoiceInputSheet (full state machine)' },
    { id: 9, name: 'TTS Voice Output', status: 'Completed', desc: 'Android TextToSpeech (Hindi), speaks balance/confirmations aloud via VoiceTtsManager' },
    { id: 10, name: 'Hindi Number Parser', status: 'Completed', desc: '"dhai sau" → 250, "dedh hazaar" → 1500, parsing + speech generation' },
    { id: 11, name: 'Smart Intent Parser', status: 'Completed', desc: 'Detect search/medicine/payment/new/correction/confirm from natural speech' },
    { id: 12, name: 'Mic-as-King Home', status: 'Completed', desc: 'Persistent bottom voice bar in MainActivity, opens VoiceInputSheet from any screen' },
    { id: 13, name: 'Fuzzy Patient Matching', status: 'Completed', desc: 'Name + alias search in findPatientByVoice, DAO findPatientsByNameOrAlias' },
    { id: 14, name: 'Conversation State Machine', status: 'Completed', desc: 'IDLE → LISTENING → PROCESSING → CONFIRMING → SAVING → DONE in VoiceInputSheet' },
    { id: 15, name: 'Disambiguation Flow', status: 'Completed', desc: '"Do Ramesh hain — kaun sa?"' },
    { id: 16, name: 'Compose Navigation Refactor', status: 'Completed', desc: 'Migrated from state-based manual fragment stack to type-safe Jetpack Compose Navigation with NavHost and Screen Sealed Class.' },
    { id: 17, name: 'Clinic Memory & Search Expansion', status: 'Completed', desc: 'Surfaced patient notes, aliases, and last update date directly in the search cards to answer who owes what instantly.' },
    { id: 18, name: 'Localization & Audit Clean', status: 'Completed', desc: 'Separation of Hindi & English. Removed bracketed bilingual texts. Unified M3 Typography and persistent language selection.' }
  ];

  const screens = [
    { name: '1. Home / Search', status: 'Completed', ref: 'Mic is King', notes: 'Recent patients, search bar with dynamic filters, instantly visible balances, aliases, notes, and last visit updates.' },
    { name: '2. Listening / Voice Input', status: 'Completed', ref: 'Flows 1-8', notes: 'VoiceInputSheet with full state machine, persistent bottom voice bar, Hindi/Hinglish transcription + confirmation.' },
    { name: '3. Add Patient Screen', status: 'Completed', ref: 'New dedicated route', notes: 'A modern, full-screen Material 3 form to add patients with real-time village dropdown selection.' },
    { name: '4. Voice Confirmation Card', status: 'Completed', ref: 'Sahi Hai? / Badlo?', notes: 'showConfirmationCard + TTS "Sahi hai?" + Haan/Nahi buttons + voice loop.' },
    { name: '5. Patient Detail', status: 'Completed', ref: 'Profile + history', notes: 'Shows name, village, phone, balance, notes, aliases, transactions, family.' },
    { name: '6. Manual Entry', status: 'Completed', ref: 'Fallback forms', notes: 'Medicine/Payment/Adjustment dialogs with amount, notes, reason.' },
    { name: '7. Analytics', status: 'Completed', ref: 'Stats dashboard', notes: 'Today/week/month collections, top patients, village breakdown.' },
    { name: '8. Settings / Backup', status: 'Completed', ref: 'Villages + backup', notes: 'Village CRUD, export/import, auto backup status, pure English/Hindi toggle.' }
  ];

  const designPrinciples = [
    { title: 'Clinic Memory Paradigm', desc: 'The app serves as the doctor\'s memory: showing aliases, families, relationships, last update, and patient notes instantly.' },
    { title: 'Voice-First, Manual-Second', desc: 'Mic is the most prominent element on screen, powered by a non-blocking in-app sheet.' },
    { title: 'Type-Safe Compose Navigation', desc: 'All app screens are driven by a centralized NavHost in MainActivity, passing arguments cleanly and safely.' },
    { title: 'One-Thumb, One-Hand', desc: 'All touch targets are designed to be at least 48dp x 48dp for comfortable everyday usage.' },
    { title: 'Running Balance & Audit Trail', desc: 'Never delete transactions; corrections are tracked via adjustments, providing an ironclad audit trail.' }
  ];

  const auditItems = [
    { category: 'Compose Navigation Migration', severity: 'High', status: 'Resolved', desc: 'Replaced manual state-based backstack with Jetpack Compose Navigation library (NavHost, NavController, Screen sealed class) for clean and robust transitions.' },
    { category: 'Add Patient View', severity: 'Medium', status: 'Resolved', desc: 'Refactored the floating dialog into a dedicated AddPatientScreen for better form usability, clear validation, and consistent design.' },
    { category: 'Clinic Memory Enrichment', severity: 'High', status: 'Resolved', desc: 'Enriched search cards to show aliases, notes, and last visit dates immediately, eliminating the need to tap into detail screens for simple inquiries.' },
    { category: 'Localization Separation', severity: 'High', status: 'Resolved', desc: 'Separated strings cleanly into English and Hindi files. Managed language switching persistently via LocaleManager.' }
  ];

  return (
    <div style={{ fontFamily: 'system-ui, sans-serif', maxWidth: '1200px', margin: '0 auto', padding: '24px', backgroundColor: '#F8FAFC', color: '#1E293B', minHeight: '100vh' }}>
      {/* Header */}
      <header style={{ borderBottom: '1px solid #E2E8F0', paddingBottom: '16px', marginBottom: '24px' }}>
        <h1 style={{ fontSize: '28px', fontWeight: 'bold', color: '#0F172A', margin: '0 0 4px 0' }}>Village Clinic Ledger</h1>
        <p style={{ fontSize: '15px', color: '#64748B', margin: '0' }}>Project Flow Dashboard & Spec</p>
      </header>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: '8px', borderBottom: '1px solid #E2E8F0', marginBottom: '24px', overflowX: 'auto' }}>
        <button 
          onClick={() => setActiveTab('phases')}
          style={{ padding: '10px 16px', border: 'none', background: 'none', borderBottom: activeTab === 'phases' ? '2px solid #3B82F6' : 'none', color: activeTab === 'phases' ? '#3B82F6' : '#64748B', fontWeight: '600', cursor: 'pointer', whiteSpace: 'nowrap' }}
        >
          Build Phases
        </button>
        <button 
          onClick={() => setActiveTab('screens')}
          style={{ padding: '10px 16px', border: 'none', background: 'none', borderBottom: activeTab === 'screens' ? '2px solid #3B82F6' : 'none', color: activeTab === 'screens' ? '#3B82F6' : '#64748B', fontWeight: '600', cursor: 'pointer', whiteSpace: 'nowrap' }}
        >
          Screens
        </button>
        <button 
          onClick={() => setActiveTab('design')}
          style={{ padding: '10px 16px', border: 'none', background: 'none', borderBottom: activeTab === 'design' ? '2px solid #3B82F6' : 'none', color: activeTab === 'design' ? '#3B82F6' : '#64748B', fontWeight: '600', cursor: 'pointer', whiteSpace: 'nowrap' }}
        >
          Design Principles
        </button>
        <button 
          onClick={() => setActiveTab('audit')}
          style={{ padding: '10px 16px', border: 'none', background: 'none', borderBottom: activeTab === 'audit' ? '2px solid #3B82F6' : 'none', color: activeTab === 'audit' ? '#3B82F6' : '#64748B', fontWeight: '600', cursor: 'pointer', whiteSpace: 'nowrap' }}
        >
          Development Updates
        </button>
      </div>

      {/* Tab Contents */}
      {activeTab === 'phases' && (
        <div>
          <h2 style={{ fontSize: '20px', fontWeight: 'bold', marginBottom: '16px' }}>Development Progress</h2>
          <div style={{ display: 'grid', gap: '12px' }}>
            {phases.map((phase) => (
              <div key={phase.id} style={{ display: 'flex', alignItems: 'center', backgroundColor: '#FFFFFF', padding: '16px', borderRadius: '12px', border: '1px solid #E2E8F0', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
                <span style={{ minWidth: '40px', fontWeight: 'bold', color: '#3B82F6' }}>#{phase.id}</span>
                <div style={{ flex: 1, paddingRight: '16px' }}>
                  <h3 style={{ margin: '0 0 4px 0', fontSize: '16px', fontWeight: 'semibold' }}>{phase.name}</h3>
                  <p style={{ margin: 0, fontSize: '14px', color: '#64748B' }}>{phase.desc}</p>
                </div>
                <span style={{ padding: '4px 10px', borderRadius: '20px', fontSize: '12px', fontWeight: '600', backgroundColor: '#DCFCE7', color: '#15803D' }}>
                  {phase.status}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {activeTab === 'screens' && (
        <div>
          <h2 style={{ fontSize: '20px', fontWeight: 'bold', marginBottom: '16px' }}>App Screens Matrix</h2>
          <div style={{ display: 'grid', gap: '16px' }}>
            {screens.map((screen, idx) => (
              <div key={idx} style={{ backgroundColor: '#FFFFFF', padding: '20px', borderRadius: '12px', border: '1px solid #E2E8F0', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                  <h3 style={{ margin: 0, fontSize: '17px', fontWeight: 'bold', color: '#0F172A' }}>{screen.name}</h3>
                  <span style={{ padding: '4px 10px', borderRadius: '20px', fontSize: '12px', fontWeight: '600', backgroundColor: '#DCFCE7', color: '#15803D' }}>
                    {screen.status}
                  </span>
                </div>
                <p style={{ margin: '0 0 12px 0', fontSize: '14px', color: '#475569' }}>{screen.notes}</p>
                <div style={{ fontSize: '12px', color: '#94A3B8', textTransform: 'uppercase', fontWeight: 'bold', letterSpacing: '0.05em' }}>
                  Antigravity Ref: <span style={{ color: '#475569' }}>{screen.ref}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {activeTab === 'design' && (
        <div>
          <h2 style={{ fontSize: '20px', fontWeight: 'bold', marginBottom: '16px' }}>Core UX Principles</h2>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '16px' }}>
            {designPrinciples.map((principle, idx) => (
              <div key={idx} style={{ backgroundColor: '#FFFFFF', padding: '20px', borderRadius: '12px', border: '1px solid #E2E8F0', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
                <h3 style={{ margin: '0 0 8px 0', fontSize: '16px', fontWeight: 'bold', color: '#0F172A' }}>{principle.title}</h3>
                <p style={{ margin: 0, fontSize: '14px', color: '#64748B', lineHeight: '1.5' }}>{principle.desc}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {activeTab === 'audit' && (
        <div>
          <div style={{ backgroundColor: '#EFF6FF', padding: '20px', borderRadius: '12px', border: '1px solid #BFDBFE', marginBottom: '24px' }}>
            <h2 style={{ fontSize: '20px', fontWeight: 'bold', color: '#1E40AF', margin: '0 0 8px 0' }}>Latest Development Achievements</h2>
            <p style={{ fontSize: '15px', color: '#1E3A8A', margin: 0 }}>
              The application has been successfully transformed into a modern offline-first clinic memory with robust navigation, centralized Compose UI routes, instant search lookups, and fully verified database performance.
            </p>
          </div>
          <div style={{ display: 'grid', gap: '16px' }}>
            {auditItems.map((item, idx) => (
              <div key={idx} style={{ backgroundColor: '#FFFFFF', padding: '20px', borderRadius: '12px', border: '1px solid #E2E8F0', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                  <h3 style={{ margin: 0, fontSize: '17px', fontWeight: 'bold', color: '#0F172A' }}>{item.category}</h3>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <span style={{ padding: '4px 10px', borderRadius: '20px', fontSize: '12px', fontWeight: '600', backgroundColor: '#FEE2E2', color: '#991B1B' }}>
                      Severity: {item.severity}
                    </span>
                    <span style={{ padding: '4px 10px', borderRadius: '20px', fontSize: '12px', fontWeight: '600', backgroundColor: '#DCFCE7', color: '#15803D' }}>
                      {item.status}
                    </span>
                  </div>
                </div>
                <p style={{ margin: 0, fontSize: '14px', color: '#475569' }}>{item.desc}</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
