import React from 'react';
import { Link } from 'react-router-dom';
import { Zap, Shield, Activity, TrendingUp, ArrowRight } from 'lucide-react';

const features = [
  { icon: Zap,       title: 'P2P Energy Trading',   desc: 'Trade surplus solar energy directly with neighbours. No middleman.' },
  { icon: Shield,    title: 'Secure Settlements',    desc: 'Blockchain-grade transaction hashes ensure tamper-proof settlements.' },
  { icon: Activity,  title: 'Real-Time Telemetry',   desc: 'Live monitoring of generation, consumption and surplus energy data.' },
  { icon: TrendingUp,title: 'Smart Matching',        desc: 'Automated order matching connects sellers and buyers instantly.' },
];

export default function LandingPage() {
  return (
    <div className="landing-root">
      {/* Hero */}
      <nav className="landing-nav">
        <div className="landing-logo"><Zap size={22} /> EcoGrid</div>
        <div className="landing-nav-links">
          <Link to="/login"    className="btn btn-ghost">Sign in</Link>
          <Link to="/register" className="btn btn-primary">Get Started</Link>
        </div>
      </nav>

      <section className="hero-section">
        <div className="hero-badge">🌱 Renewable Energy P2P Platform</div>
        <h1 className="hero-title">
          Trade Clean Energy<br />
          <span className="hero-accent">Peer to Peer</span>
        </h1>
        <p className="hero-subtitle">
          EcoGrid connects prosumers who generate solar power with consumers
          who need it — directly, transparently, and sustainably.
        </p>
        <div className="hero-cta">
          <Link to="/register" className="btn btn-primary btn-lg">
            Start Trading <ArrowRight size={18} />
          </Link>
          <Link to="/login" className="btn btn-outline btn-lg">Sign In</Link>
        </div>
      </section>

      {/* Stats strip */}
      <div className="landing-stats">
        {[['10 kW+','Average Solar Capacity'], ['₹4.20','Avg Price / kWh'], ['99.9%','Uptime SLA'], ['SHA-256','Transaction Hash']].map(([val, lbl]) => (
          <div key={lbl} className="landing-stat">
            <span className="landing-stat-val">{val}</span>
            <span className="landing-stat-lbl">{lbl}</span>
          </div>
        ))}
      </div>

      {/* Features */}
      <section className="features-section">
        <h2 className="section-title">Why EcoGrid?</h2>
        <div className="features-grid">
          {features.map(({ icon: Icon, title, desc }) => (
            <div className="feature-card" key={title}>
              <div className="feature-icon"><Icon size={26} /></div>
              <h3 className="feature-title">{title}</h3>
              <p className="feature-desc">{desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Footer CTA */}
      <section className="landing-cta-section">
        <h2>Ready to power the future?</h2>
        <p>Join the EcoGrid community and start trading renewable energy today.</p>
        <Link to="/register" className="btn btn-primary btn-lg">Create Free Account</Link>
      </section>

      <footer className="landing-footer">
        <span><Zap size={14} /> EcoGrid P2P Energy Trading Platform</span>
        <span>Built with Spring Boot Microservices + React</span>
      </footer>
    </div>
  );
}
