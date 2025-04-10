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


    function toggleDetails(id, button) {
        /* 
        let element = document.getElementById(id);
        let currentDisplay = window.getComputedStyle(element).display;
        if (currentDisplay === "none") {
            element.style.display = "block";
        } else {
            element.style.display = "none";
        }
            */
        const el = document.getElementById(id);
        el.classList.toggle('active');
        
        // Меняем текст кнопки в зависимости от состояния
        if (el.classList.contains('active')) {
            button.textContent = '➖';
        } else {
            button.textContent = '➕';
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
            build3DWarehouse(solution.method, solution.solution, solution.relativeShelving);
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

        document.getElementById('scoreChart').style.display = 'block';
        document.getElementById('timeChart').style.display = 'block';
    }




    function build3DWarehouse(method, data, relativeShelving) {
        // мин/макс для последующей нормализации расстояния
        let maxDist = 0;
        let minDist = Infinity;
        // вес для учета относительного уровня ячейки
        const levelWeight = 0.3;
        // верхнии границы классов для ABC анализа (спрос, расстояние)
        const classA = 0.2;
        const classB = 0.3;

        // границы классов для ABC анализа (срок годности). Временные промежутки    
        class TimeInterval {
            constructor(days) {
                this.days = days;
                this.milliseconds = days * 24 * 60 * 60 * 1000; // Преобразуем дни в миллисекунды
            }
        }
        const shortTerm = new TimeInterval(2) // скоропорт 0,5 до 30 суток
        const midTerm = new TimeInterval(30) // среднесрок от 30 до 180 суток
        // const longTerm = new TimeInterval(190) // долгосрок 
        // общая точка отсчета (текущее время)
        const currentTime = new Date();




        // Вычисляем max и min расстояния
        for (const pair of data) {
            const cell = pair.cell;
            const dist = Math.abs(cell.coordinates.x - assemblyPoint.x) + Math.abs(cell.coordinates.y - assemblyPoint.y) +
                levelWeight * relativeShelving[cell.coordinates.z];
            if (dist > maxDist) maxDist = dist;
            if (dist < minDist) minDist = dist;
        }

        // Данные для графика
        const xCells = data.map(pair => pair.cell.coordinates.x);
        const yCells = data.map(pair => pair.cell.coordinates.y);
        const zCells = data.map(pair => pair.cell.coordinates.z);

        const products = data.map(pair => pair.product);
        // кластеризация товаров по спросу
        const prodClusters = products.map(prod => {
            if (prod.demand / 100 >= 1 - classA) {
                // чем больше спрос - тем лучше
                prod.cluster = "A";
                return "red";
            }
            else if (prod.demand / 100 < 1 - classA && prod.demand / 100 >= 1 - classB) {
                prod.cluster = "B";
                return "green";
            }
            else {
                prod.cluster = "C";
                return "blue";
            }
        })

        // кластеризация товаров по срокам годности
        const prodClustersByExpiryDate = products.map(prod => {
            const prodExpiryDate = new Date(prod.expiryDate);
            const remained = prodExpiryDate.getTime() - currentTime.getTime();
            const daysRemained = Math.round(remained / 24 / 60 / 60 / 1000);
            if (remained <= shortTerm.milliseconds) {
                return {
                    id: prod.id,
                    expiryDate: prodExpiryDate,
                    daysRemained: daysRemained,
                    cluster: "short term",
                    color: "red"
                }
            }
            else if (remained > shortTerm.milliseconds && remained <= midTerm.milliseconds) {
                return {
                    id: prod.id,
                    expiryDate: prodExpiryDate,
                    daysRemained: daysRemained,
                    cluster: "medium term",
                    color: "green"
                }
            }
            else {
                return {
                    id: prod.id,
                    expiryDate: prodExpiryDate,
                    daysRemained: daysRemained,
                    cluster: "long term",
                    color: "blue"
                };
            }
        })


        // Распределение по кластерам (по расстоянию)
        //Визуализация, вариант 1
        const cellClusters = data.map(pair => {
            const cell = pair.cell;
            const relDist = ((Math.abs(assemblyPoint.x - cell.coordinates.x) + Math.abs(assemblyPoint.y - cell.coordinates.y) + levelWeight * relativeShelving[cell.coordinates.z]) - minDist) / (maxDist - minDist);
            if (relDist <= classA) {
                cell.cluster = "A";
                return "red";
            }
            else if (relDist > classA && relDist <= classB) {
                cell.cluster = "B";
                return "green";
            }
            else {
                cell.cluster = "C";
                return "blue";
            }
        });

        // Создаем новый div для каждого метода
        const divId = `warehouse3d_${method}`;
        let container = document.getElementById(divId);
        if (!container) {
            container = document.createElement("div");
            container.id = divId;
            container.style = "width: 100%; height: 500px; margin: 0 auto;";
            document.getElementById("3d_charts").appendChild(container); // Добавляем в родительский контейнер
        } else container.style.display = "block";



        // Построение 3D графика для метода
        // кластеризация по расстоянию между ячейкой и точкой консолидации
        //________________вариант 1_________________
        //__по идее, эта кластеризация (ячеек) не должна меняться от метода к методу
        const clusteredCells = {
            x: xCells,
            y: yCells,
            z: zCells,
            mode: "markers",
            type: "scatter3d",
            name: "Кластеризация ячеек по удаленности от места консолидации",
            marker: {
                size: 6,
                color: cellClusters, // Массив кластеров (A, B, C)
                colorscale: "Viridis", // Цветовая шкала
                opacity: 0.8
            },
            text: data.map(pair => `Ячейка: ${pair.cell.id}<br>Кластер: ${pair.cell.cluster}`)
        }
        //________________вариант 2_________________
        const clusteredProducts = {
            x: xCells,
            y: yCells,
            z: zCells,
            mode: "markers",
            type: "scatter3d",
            name: "Кластеризация товаров по спросу",
            marker: {
                size: 6,
                color: prodClusters, // Массив кластеров (A, B, C)
                colorscale: "Viridis", // Цветовая шкала
                opacity: 0.8
            },
            text: products.map(product => `Товар: ${product.id}<br>Кластер: ${product.cluster}<br>Спрос: ${product.demand}`)
        }

        //________________вариант 3_________________
        const clusteredProductsByTerm = {
            x: xCells,
            y: yCells,
            z: zCells,
            mode: "markers",
            type: "scatter3d",
            name: "Кластеризация товаров по дате хранения товара",
            marker: {
                size: 6,
                color: prodClustersByExpiryDate.map(product => product.color),
                colorscale: "Viridis", // Цветовая шкала
                opacity: 0.8
            },
            text: prodClustersByExpiryDate.map(product => `Товар: ${product.id}<br>Кластер: ${product.cluster}<br>Осталось, дней (${product.daysRemained})`)
        }

        //________________точка консолидации_________________
        const assemblyPointPoints = {
            x: [assemblyPoint.x],
            y: [assemblyPoint.y],
            z: [assemblyPoint.z],
            mode: "markers",
            type: "scatter3d",
            name: "Точка консолидации",
            marker: {
                size: 8,
                color: "yellow",
                colorscale: "Viridis", // Цветовая шкала
                opacity: 0.8
            },
            text: "Точка консолидации"
        }



        Plotly.newPlot(divId, [clusteredCells, clusteredProducts, clusteredProductsByTerm, assemblyPointPoints], {
            title: { text: `3D Визуализация размещения на складе для метода ${method}` },
            scene: {
                xaxis: { title: "X" },
                yaxis: { title: "Y" },
                zaxis: { title: "Z" }
            },
            autosize: true,
            showlegend: true,
            legend: {
                yanchor: 'bottom',  // Расположение снизу
                y: -0.2,  // Смещаем легенду вниз
                xanchor: 'center',
                x: 0.5
            }

        });

        /*
        Plotly.relayout(divId, {
            width: window.innerWidth,  // Ширина окна браузера
            height: 500
        });
        */
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
