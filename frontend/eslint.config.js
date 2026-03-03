import globals from "globals";
import tseslint from "typescript-eslint";
import reactPlugin from "eslint-plugin-react";

export default [
    {
        ignores: ["dist/**"],
    },
    ...tseslint.configs.recommended,
    {
        files: ["**/*.{js,mjs,cjs,ts,jsx,tsx}"],
        plugins: {
            react: reactPlugin
        },
        languageOptions: {
            globals: {
                ...globals.browser,
            }
        },
        rules: {
            "react/react-in-jsx-scope": "off",
            "@typescript-eslint/no-explicit-any": "off",
        },
        settings: {
            react: {
                version: "detect"
            }
        }
    }
];
