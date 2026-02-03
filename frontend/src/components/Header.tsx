import React from 'react';
import {TrendingUp} from 'lucide-react';

const Header: React.FC = () => {
    return (
        <header className="bg-slate-900 text-white p-4 shadow-md flex items-center gap-2 border-b border-slate-700">
            <TrendingUp className="text-blue-400"/>
            <h1 className="text-xl font-bold tracking-tight" id="app-title">Alpha Flow</h1>
        </header>
    );
};

export default Header;
