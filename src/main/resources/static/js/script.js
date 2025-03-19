function toggleDetails(id) {
            let element = document.getElementById(id);
            if (element.style.display === "none") {
                element.style.display = "block";
            } else {
                element.style.display = "none";
            }
        }

document.addEventListener("DOMContentLoaded", function () {
            function sendRequest() {
                const selectedMethods = [];
                const allowedMethods = new Set(["RANDOM", "ELECTRE_TRI", "TOPSIS", "ELECTRE_TRI_TOPSIS", "GA"]);

                document.querySelectorAll("input[type='checkbox']:checked").forEach(cb => {
                    if (allowedMethods.has(cb.id)) {
                        selectedMethods.push(cb.id);
                    }
                });

                const fileInput = document.getElementById("warehouseConfig");
                const file = fileInput.files[0];



                if (!file) {
                    alert("Выберите файл конфигурации склада!");
                    return;
                }


                const reader = new FileReader();
                reader.onload = function (event) {
                    const warehouseConfig = JSON.parse(event.target.result);
                    const requestData = {
                        methods: selectedMethods,
                        warehouseConfig: warehouseConfig
                    };

                    // Формируем параметры в URL
                            const params = new URLSearchParams({
                                generateCells: document.getElementById("generateCells").checked,
                                cellsAmount: document.getElementById("cellsAmount").value,
                                generateProducts: document.getElementById("generateProducts").checked,
                                productsAmount: document.getElementById("productsAmount").value
                            });

                    fetch(`/api/distribute?${params.toString()}`, {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify(requestData)
                    })
                    .then(response => response.json())
                    .then(data => updateResults(data))
                    .catch(error => console.error("Ошибка:", error));
                };

                reader.readAsText(file);
            }

            function updateResults(data) {
                let resultsDiv = document.getElementById("results");
                resultsDiv.innerHTML = "";

                data.forEach(result => {
                    let div = document.createElement("div");
                    div.innerHTML = `<h3>${result.method}</h3>
                                    <p>Score: ${result.score.toFixed(2)}</p>
                                    <p>Затрачено времени: ${result.timeRequired} ms</p>
                                    <button onclick="downloadSolution('${result.method}', ${JSON.stringify(result.solution)})">Скачать JSON</button>`;
                    resultsDiv.appendChild(div);
                });
            }

            function downloadSolution(method, solution) {
                const blob = new Blob([JSON.stringify(solution, null, 2)], { type: "application/json" });
                const link = document.createElement("a");
                link.href = URL.createObjectURL(blob);
                link.download = `${method}_solution.json`;
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
            }

            window.sendRequest = sendRequest;
            window.downloadSolution = downloadSolution;
        });
