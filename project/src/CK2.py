import pyaudio
import numpy as np

# settings
sampling_rate = 48000
duration = 10
f1 = 1000
f2 = 10000

# init an audio HW
audioDriver = pyaudio.PyAudio()

# sound stream
stream = audioDriver.open(format=pyaudio.paFloat32,
                        channels=1,
                        rate=sampling_rate,
                        output=True)

# sound track:  f(t) = sin (2pi 1000 t) + sin (2pi 10000 t)
track = np.sin(2*np.pi * f1*np.arange(sampling_rate*duration)/sampling_rate) + \
        np.sin(2*np.pi * f2*np.arange(sampling_rate*duration)/sampling_rate)
samples = track.astype(np.float32)

# write to stream
stream.write(samples.tobytes())

# play
stream.stop_stream()
stream.close()
audioDriver.terminate()