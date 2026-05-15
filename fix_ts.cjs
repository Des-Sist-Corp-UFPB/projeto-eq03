const fs = require('fs');
const path = require('path');

function walk(dir, callback) {
    fs.readdirSync(dir).forEach(f => {
        let dirPath = path.join(dir, f);
        let isDirectory = fs.statSync(dirPath).isDirectory();
        if (isDirectory) walk(dirPath, callback);
        else callback(path.join(dir, f));
    });
}

function replaceInFile(filePath, replacements) {
    if (!fs.existsSync(filePath)) return;
    let content = fs.readFileSync(filePath, 'utf8');
    let original = content;
    for (const [search, replace] of replacements) {
        if (search instanceof RegExp) {
            content = content.replace(search, replace);
        } else {
            content = content.split(search).join(replace);
        }
    }
    if (content !== original) {
        fs.writeFileSync(filePath, content);
    }
}

// Fix api paths
replaceInFile('salon-front/src/pages/admin/cashflow/services/cashflow.ts', [
    ["import api from '../../../services/api';", "import api from '../../../../services/api';"]
]);
replaceInFile('salon-front/src/pages/admin/reports/services/reports.ts', [
    ["import api from '../../../services/api';", "import api from '../../../../services/api';"]
]);

// Fix tooltip formatter
replaceInFile('salon-front/src/pages/admin/reports/Reports.tsx', [
    ["(val: number) =>", "(val: any) =>"],
    ["val.toFixed(2)", "Number(val).toFixed(2)"]
]);

// Fix type imports across the project
walk('salon-front/src', (filePath) => {
    if (!filePath.endsWith('.ts') && !filePath.endsWith('.tsx')) return;
    let content = fs.readFileSync(filePath, 'utf8');
    let original = content;

    // ReactNode
    content = content.replace(/import\s+\{([^}]*)\bReactNode\b([^}]*)\}\s+from\s+['"]react['"];/g, (match, p1, p2) => {
        let otherImports = [p1, p2].join('').replace(/,/g, ' ').trim().split(/\s+/).filter(Boolean);
        if (otherImports.length === 0) {
            return `import type { ReactNode } from 'react';`;
        }
        return `import { ${otherImports.join(', ')} } from 'react';\nimport type { ReactNode } from 'react';`;
    });

    // Types from local services
    content = content.replace(/import\s+\{([^}]*)\b(CashFlowData|EmployeeData|ProductData|FinancialReportResponse|AppointmentReportResponse|ServiceData|UserData|UserUpdateRequest|AppointmentResponse|TimeSlotResponse)\b([^}]*)\}\s+from\s+['"]([^'"]+)['"];/g, 
    (match, p1, p2, p3, p4) => {
        let otherImports = [p1, p3].join('').replace(/,/g, ' ').trim().split(/\s+/).filter(Boolean);
        if (otherImports.length === 0) {
            return `import type { ${p2} } from '${p4}';`;
        }
        return `import { ${otherImports.join(', ')} } from '${p4}';\nimport type { ${p2} } from '${p4}';`;
    });

    // JwtPayload, UserContextData in AuthContext
    if (filePath.endsWith('AuthContext.tsx')) {
        content = content.replace(/import\s+\{([^}]*)\bJwtPayload\b([^}]*)\}\s+from/g, `import type { JwtPayload } from`);
        content = content.replace(/import\s+\{([^}]*)\bUserContextData\b([^}]*)\}\s+from\s+['"]([^'"]+)['"]/g, `import type { UserContextData } from '$3'`);
        content = content.replace(/import api from '..\/services\/api';\n/g, ''); // Unused api
    }

    // InternalAxiosRequestConfig
    if (filePath.endsWith('api.ts')) {
        content = content.replace(/import axios, \{ InternalAxiosRequestConfig \} from 'axios';/, `import axios from 'axios';\nimport type { InternalAxiosRequestConfig } from 'axios';`);
    }

    if (content !== original) {
        fs.writeFileSync(filePath, content);
    }
});

console.log("Types fixed");
