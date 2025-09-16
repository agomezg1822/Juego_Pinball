from flask import Flask, request
import threading
import matplotlib.pyplot as plt
import matplotlib.animation as animation
import numpy as np

coords = []
coords_lock = threading.Lock()

app = Flask(__name__)

@app.route("/puntos")
def puntos():
    print("¡Ganaste! alcanzaste los 20 puntos!!, 50 profe!")
    return "OK", 200


@app.route("/dibujo")
def dibujo():
    x = float(request.args.get("x", 0))
    y = float(request.args.get("y", 0))
    # proteger acceso concurrente a coords
    with coords_lock:
        coords.append((x, y))
    return "OK", 200

def live_plot():
    fig, ax = plt.subplots()
    line, = ax.plot([], [], 'k-')
    ax.set_xlim(0, 1080)   # ajusta al tamaño de tu móvil
    ax.set_ylim(1920, 0)   # invertido para que coincida con Android

    def update(frame):
        with coords_lock:
            if coords:
                xs, ys = zip(*coords)
                line.set_data(xs, ys)
        return line,

    ani = animation.FuncAnimation(fig, update, interval=100)
    plt.show()


@app.route("/estrella")
def estrella():
    print("✨ Iniciando animación de estrella...")

    def plot_star():
        fig, ax = plt.subplots()
        ax.set_xlim(-2, 2)
        ax.set_ylim(-2, 2)

        # Coordenadas de una estrella de 5 puntas (10 vértices alternando radio)
        theta = np.linspace(0, 2*np.pi, 11)
        r = np.array([1, 0.4] * 5 + [1])
        x = r * np.cos(theta)
        y = r * np.sin(theta)
        line, = ax.plot(x, y, 'g-')

        def update(frame):
            angle = np.radians(frame)
            rot_x = x * np.cos(angle) - y * np.sin(angle)
            rot_y = x * np.sin(angle) + y * np.cos(angle)
            line.set_data(rot_x, rot_y)
            return line,

        ani = animation.FuncAnimation(fig, update, interval=50, blit=True)
        plt.show()

    threading.Thread(target=plot_star, daemon=True).start()
    return "OK", 200

if __name__ == "__main__":
    # Ejecutar Flask en hilo aparte para que matplotlib pueda correr en el hilo principal (necesario en macOS)
    def run_flask():
        app.run(host="0.0.0.0", port=6800, debug=False, use_reloader=False)

    flask_thread = threading.Thread(target=run_flask, daemon=True)
    flask_thread.start()

    # Ejecutar la interfaz matplotlib en el hilo principal
    live_plot()
  