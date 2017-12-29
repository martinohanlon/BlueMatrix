class Colour():
    def __init__(self, red = 255, green = 255, blue = 255, alpha = 0):
        self._red = red
        self._green = green
        self._blue = blue
        self._alpha = alpha

    @property
    def red(self):
        return self._red

    @red.setter
    def red(self, value):
        self._red = value

    @property
    def green(self):
        return self._green

    @green.setter
    def green(self, value):
        self._green = value

    @property
    def blue(self):
        return self._blue

    @blue.setter
    def blue(self, value):
        self._blue = value

    @property
    def alpha(self):
        return self._alpha

    @green.setter
    def alpha(self, value):
        self._alpha = value

    @property
    def rgb(self):
        return (self._red, self._green, self._blue)

    @rgb.setter
    def rgb(self, value):
        self._red = value[0]
        self._green = value[1]
        self._blue = value[2]

    @property
    def rgba(self):
        return (self._red, self._green, self._blue, self._alpha)

    @rgb.setter
    def rgba(self, value):
        self._red = value[0]
        self._green = value[1]
        self._blue = value[2]
        self._alpha = value[3]

    @property
    def str_rgb(self):
        return '#%02x%02x%02x' % (self._red, self._green, self._blue)

    @property
    def str_rgba(self):
        return '#%02x%02x%02x%02x' % (self._red, self._green, self._blue, self._alpha)

    def __str__(self):
        return self.str_rgba


WHITE = Colour(255,255,255)
BLACK = Colour(0,0,0)
RED = Colour(255,0,0)
GREEN = Colour(0,255,0)
BLUE = Colour(0,0,255)
YELLOW = Colour(255,255,0)
GREY = Colour(102,102,102)