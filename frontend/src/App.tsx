import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import { AppRouter } from "./app/router";

const queryClient = new QueryClient({
    defaultOptions: {
        mutations: {
            retry: false,
        },
        queries: {
            retry: false,
        },
    },
});

function App() {
    return (
        <QueryClientProvider client={queryClient}>
            <BrowserRouter>
                <AppRouter />
            </BrowserRouter>
        </QueryClientProvider>
    );
}

export default App;
