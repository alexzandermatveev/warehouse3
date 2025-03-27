'use strict';
let fileLoaded = false; // Флаг, загружался ли JSON из файла
//точка сбора
let assemblyPoint = {
    "x": 0,
    "y": 0,
    "z": 0
};

document.addEventListener("DOMContentLoaded", function () {
    const jsonError = document.getElementById("jsonError");
    // Подключаем CodeMirror к textarea
    const editor = CodeMirror.fromTextArea(document.getElementById("jsonTextArea"), {
        mode: { name: "javascript", json: true },
        theme: "default",
        lineNumbers: true,
        autoCloseBrackets: true,
        matchBrackets: true,
    });

    function toggleConstructor() {
        const constructorDiv = document.getElementById("warehouseConstructor");
        constructorDiv.style.display = constructorDiv.style.display === "none" ? "block" : "none";
    }

    function generateJsonFromConstructor() {
        const config = {
            Warehouse: {
                id: document.getElementById("whId").value,
                assemblyPoint: {
                    x: parseInt(document.getElementById("apX").value),
                    y: parseInt(document.getElementById("apY").value),
                    z: parseInt(document.getElementById("apZ").value)
                },
                levels: parseInt(document.getElementById("levels").value),
                totalCells: parseInt(document.getElementById("totalCells").value),
                cellSize: {
                    width: parseInt(document.getElementById("cellWidth").value),
                    height: parseInt(document.getElementById("cellHeight").value),
                    depth: parseInt(document.getElementById("cellDepth").value)
                },
                cells: []
            },
            products: []
        };

        const formattedJson = JSON.stringify(config, null, 2);
        editor.setValue(formattedJson); // вставляем в CodeMirror
    }




    function downloadEditorJson() {
        const jsonContent = editor.getValue(); // Получаем JSON из CodeMirror

        try {
            // Проверим, валидный ли JSON
            JSON.parse(jsonContent);

            const blob = new Blob([jsonContent], { type: "application/json" });
            const link = document.createElement("a");
            link.href = URL.createObjectURL(blob);
            link.download = "warehouse_config.json";
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        } catch (e) {
            alert("содержимое редактора не является корректным JSON!");
        }
    }


    function toggleDetails(id) {
        let element = document.getElementById(id);
        let currentDisplay = window.getComputedStyle(element).display;
        if (currentDisplay === "none") {
            element.style.display = "block";
        } else {
            element.style.display = "none";
        }
    }



    function validateJson() {
        try {
            JSON.parse(editor.getValue());
            jsonError.textContent = ""; // Если JSON валиден, очищаем сообщение
        } catch (error) {
            jsonError.textContent = "Ошибка: введенные данные не являются корректным JSON!";
        }
    }

    // Проверяем JSON при каждом изменении
    editor.on("change", validateJson);



    function clearJson() {
        if (fileLoaded) { // Если содержимое из файла, удаляем файл
            editor.setValue("");
            const fileInput = document.getElementById("warehouseConfig");
            fileInput.value = ""; // Сбрасываем input type="file"
            fileLoaded = false; // Сбрасываем флаг
        }
        else {
            document.getElementById("warehouseConfig").value = "";
        }
    }


    function loadJsonFromFile(event) {
        const file = event.target.files[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = function (e) {
            try {
                const json = JSON.parse(e.target.result);
                editor.setValue(JSON.stringify(json, null, 2)); // Форматированный JSON
                fileLoaded = true; // Фиксируем, что JSON загружен из файла

            } catch (error) {
                alert("Ошибка: неверный формат JSON!");
            }
        };
        reader.readAsText(file);
    }

    function sendRequest() {
        const jsonText = editor.getValue();
        if (!jsonText.trim()) {
            alert("Поле JSON не должно быть пустым!");
            return;
        }

        const selectedMethods = [];
        const allowedMethods = new Set(["RANDOM", "ELECTRE_TRI", "TOPSIS", "ELECTRE_TRI_TOPSIS", "GA"]);

        document.querySelectorAll("input[type='checkbox']:checked").forEach(cb => {
            if (allowedMethods.has(cb.id)) {
                selectedMethods.push(cb.id);
            }
        });

        if (selectedMethods.length < 1) {
            alert("Ни один метод не выбран");
            return;
        }

        try {
            const jsonData = JSON.parse(jsonText); // Проверяем, что JSON валиден

            //перезаписываем точку сбора
            assemblyPoint = jsonData.Warehouse.assemblyPoint;

            const requestData = {
                methods: selectedMethods,
                warehouseConfig: jsonData // JSON, а не строка
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
                .then(async response => {
                    if (!response.ok) {
                        const errorText = await response.text(); // Получаем тело ошибки
                        throw new Error(`Ошибка:\n${errorText}`);
                    }
                    return response.json();
                })
                .then(data => updateResults(data))
                .catch(error => {
                    console.error("Ошибка запроса:", error);
                    alert(error.message);
                });
        } catch (error) {
            alert("Ошибка: введенные данные не являются корректным JSON!");
        }
    }

    function updateResults(data) {
        //console.log("Данные в updateResults", data);

        buildCharts(data);
        data.forEach(solution => {
            build3DWarehouse(solution.method, solution.solution);
        });
        let resultsDiv = document.getElementById("results");
        resultsDiv.innerHTML = "";

        data.forEach(result => {
            let div = document.createElement("div");
            div.innerHTML = `<h3>${result.method}</h3>
                             <p>Score: ${result.score.toFixed(2)}</p>
                             <p>Затрачено времени: ${result.timeRequired} ms</p>
                             <button class="download-btn" data-method="${result.method}" data-solution='${JSON.stringify(result.solution)}'>
                                Скачать JSON
                             </button>`;

            resultsDiv.appendChild(div);
        });

        // Назначаем обработчик событий для кнопок
        document.querySelectorAll(".download-btn").forEach(button => {
            button.addEventListener("click", function () {
                const method = this.getAttribute("data-method");
                const solution = this.getAttribute("data-solution");
                downloadSolution(method, solution);
            });
        });
    }

    function buildCharts(data) {

        //console.log("Данные для построения графиков:", data); // Проверка данных
        // Данные для графиков
        data.sort((a, b) => b.score - a.score);

        const methods = data.map(result => result.method);
        const scores = data.map(result => result.score);

        let layout = {
            title: { text: "Показатель работы для каждого плана" },
            xaxis: {
                tickangle: -45
            },
        };

        Plotly.newPlot("scoreChart", [{
            x: methods,
            y: scores,
            type: "bar",
            name: "Score",
            marker: { color: "teal" }
        }], layout);



        data.sort((a, b) => b.timeRequired - a.timeRequired);

        const methods2 = data.map(result => result.method);
        const times2 = data.map(result => result.timeRequired);

        layout.title.text = "Затраченное на решение время, мс";
        layout["yaxis"] = { title: { text: "Время, мс" } };
        console.log(layout);

        Plotly.newPlot("timeChart", [{
            x: methods2,
            y: times2,
            type: "bar",
            name: "Время, мс",
            marker: { color: "orange" }
        }], layout);
    }




    function build3DWarehouse(method, data) {
        let maxDist = 0;
        let minDist = Infinity;



        // Вычисляем max и min расстояния
        for (const pair of data) {
            const cell = pair.cell;
            const dist = Math.abs(cell.coordinates.x - assemblyPoint.x) + Math.abs(cell.coordinates.y - assemblyPoint.y) +
                Math.abs(cell.coordinates.x - assemblyPoint.z);
            if (dist > maxDist) maxDist = dist;
            if (dist < minDist) minDist = dist;
        }

        // Данные для графика
        const x = data.map(pair => pair.cell.coordinates.x);
        const y = data.map(pair => pair.cell.coordinates.y);
        const z = data.map(pair => pair.cell.coordinates.z);

        // Распределение по кластерам (по расстоянию)
        const clusters = data.map(pair => {
            const cell = pair.cell;
            const relDist = (maxDist - (Math.abs(assemblyPoint.x - cell.coordinates.x) + Math.abs(assemblyPoint.y - cell.coordinates.y) + Math.abs(assemblyPoint.z - cell.coordinates.z))) / (maxDist - minDist);
            if (relDist <= 0.2) {
                cell.cluster = "A";
                return "red";
            }
            else if (relDist > 0.2 && relDist <= 0.3) {
                cell.cluster = "B";
                return "green";
            }
            else {
                cell.cluster = "C";
                return "blue";
            }
        });

        // Создаём новый div для каждого метода
        const divId = `warehouse3d_${method}`;
        let container = document.getElementById(divId);
        if (!container) {
            container = document.createElement("div");
            container.id = divId;
            container.style = "width: 100%; height: 500px; margin-bottom: 20px;";
            document.getElementById("3d_charts").appendChild(container); // Добавляем в родительский контейнер
        } else container.style.display = "block";

        // Построение 3D графика для метода
        Plotly.newPlot(divId, [{
            x: x,
            y: y,
            z: z,
            mode: "markers",
            type: "scatter3d",
            marker: {
                size: 6,
                color: clusters, // Массив кластеров (A, B, C)
                colorscale: "Viridis", // Цветовая шкала
                opacity: 0.8
            },
            text: data.map(cell => `Ячейка: ${cell.cell.id}<br>Кластер: ${cell.cell.cluster}`)
        }], {
            title: { text: `3D Визуализация склада для метода ${method}` },
            scene: {
                xaxis: { title: "X" },
                yaxis: { title: "Y" },
                zaxis: { title: "Z" }
            },
            showlegend: true
        });
    }



    function downloadSolution(method, solution) {
        try {
            const jsonSolution = JSON.stringify(JSON.parse(solution), null, 2); // Форматируем JSON
            const blob = new Blob([jsonSolution], { type: "application/json" });
            const link = document.createElement("a");
            link.href = URL.createObjectURL(blob);
            link.download = `${method}_solution.json`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        } catch (error) {
            console.error("Ошибка при скачивании JSON:", error);
        }
    }


    // Привязываем обработчик загрузки файла после загрузки DOM
    document.getElementById("warehouseConfig").addEventListener("change", loadJsonFromFile);

    // Привязываем кнопку "Отправить"
    document.getElementById("sendButton").addEventListener("click", sendRequest);

    // document.getElementById("openConstructor").addEventListener("click", toggleConstructor);


    // Делаем функции глобальными (если нужно вызывать их в HTML)
    window.toggleDetails = toggleDetails;
    window.clearJson = clearJson;
    window.downloadSolution = downloadSolution;
    window.generateJsonFromConstructor = generateJsonFromConstructor;
    window.toggleConstructor = toggleConstructor;
    window.downloadEditorJson = downloadEditorJson;
});
