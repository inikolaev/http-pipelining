import socket
import ssl
import time


def create_request(host, path, close=False):
    return "GET {} HTTP/1.1\r\nHost: {}\r\nConnection: {}\r\n\r\n".format(path, host, "close" if close else "keep-alive")


def request_generator(host, path, size):
    for i in range(size):
        yield create_request(host, path, i == size - 1)


def count_responses(responses):
    full_response = "".join(r.decode() for r in responses)

    pos = 0
    count = 0
    while True:
        pos = full_response.find("HTTP/1.1 302 Found", pos)

        if pos == -1:
            break

        pos += 15
        count += 1

    return count


host = "www.google.com"
path = "/"
request = "".join(request_generator(host, path, 127)).encode()
response = []

sock = ssl.wrap_socket(socket.socket(socket.AF_INET, socket.SOCK_STREAM))
sock.connect((host, 443))

send = time.clock()
sock.send(request)

recv = time.clock()
ttfb = None

while True:
    buffer = sock.recv(64 * 1024)

    if not buffer:
        break

    if not ttfb:
        ttfb = time.clock()

    response.append(buffer)

end = time.clock()
sock.close()

print("Responses received: {}".format(count_responses(response)))
print("Send time:          {}".format(recv - send))
print("Time to first byte: {}".format(ttfb - recv))
print("Receive time:       {}".format(end - recv))
print("Total:              {}".format(end - send))
