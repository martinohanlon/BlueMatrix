from colours import BLUE
import json

class BlueMatrix():
    def __init__(self, cols = 5, rows = 3, border = True, visible = True, colour = BLUE):
        self._cols = 0
        self._rows = 0
        self._border = border
        self._visible = visible
        self._colour = colour
        self._cells = {}
        self._update_cells(cols, rows)
        
    def _update_cells(self, cols, rows):
        # create new cells
        new_cells = {}

        for c in range(cols):
            for r in range(rows):
                # if cell already exist, reuse it
                if (c,r) in self._cells.keys():
                    new_cells[c,r] = self._cells[(c,r)]
                else:   
                    new_cells[c,r] = BlueMatrixCell(c, r, self._border, self._visible, self._colour)
                
        self._cols = cols
        self._rows = rows
        self._cells = new_cells
    
    # METHODS
    def cell(self, col, row):
        try:
            return self._cells[(col, row)]
        except KeyError as e:
            raise KeyError("Cell ({},{}) does not exist".format(col, row))

    def resize(self, cols, rows):
        self._update_cells(cols, rows)

    # PROPERTIES
    @property
    def cells(self):
        return [ c for c in self._cells.values() ]

    @property
    def cols(self):
        return self._cols

    @cols.setter
    def cols(self, value):
        self._update_cells(value, self._rows)

    @property
    def rows(self):
        return self._rows

    @rows.setter
    def rows(self, value):
        self._update_cells(self._cols, rows)

    @property
    def colour(self):
        return self._colour

    @colour.setter
    def colour(self, value):
        if self._colour != value:
            self._colour = value
            # update all the cells
            for cell in self.cells:
                cell.colour = value

    @property
    def border(self):
        return self._border

    @border.setter
    def border(self, value):
        self._border = value
        for cell in self.cells:
            cell.border = value

    @property
    def visible(self):
        return self._visible

    @visible.setter
    def visible(self, value):
        self._visible = value
        for cell in self.cells:
            cell.visible = value

    def __str__(self):
        return "cols = {}, rows = {}, border = {}, visible = {}, colour = {}".format(self._col, self._row, self._border, self._visible, self._colour)


class BlueMatrixCell():
    def __init__(self, col, row, border, visible, colour):
        self._col = col
        self._row = row
        self._border = border
        self._visible = visible
        self._colour = colour

    #PROPERTIES
    @property
    def col(self):
        return self._col

    @col.setter
    def col(self, value):
        self._col = col

    @property
    def row(self):
        return self._row

    @row.setter
    def row(self, value):
        self._row = value

    @property
    def colour(self):
        return self._colour

    @colour.setter
    def colour(self, value):
        self._colour = value

    @property
    def border(self):
        return self._border

    @border.setter
    def border(self, value):
        self._border = value

    @property
    def visible(self):
        return self._visible

    @visible.setter
    def visible(self, value):
        self._visible = value

    def __str__(self):
        return "({},{}), border = {}, visible = {}, colour = {}".format(self._col, self._row, self._border, self._visible, self._colour)


class CommandBuilder():
    
    def __init__(self, bm):
        self._bm = bm

    def all(self):
        cmd = {}
        self._matrix(cmd)
        self._cells(cmd)
        return cmd

    def matrix(self):
        cmd = {}
        self._matrix(cmd)
        return cmd

    def _matrix(self, cmd):
        cmd["matrix"] = {}
        cmd["matrix"]["cols"] = self._bm.cols
        cmd["matrix"]["rows"] = self._bm.rows
        return cmd

    def cells(self):
        cmd = {}
        self._cells(cmd)
        return cmd

    def _cells(self, cmd):
        cmd["cell"] = []
        for cell in self._bm.cells:
            cmd["cell"].append(self._cell(cell))
        return c

    def cell(self, col, row):
        cmd = {}
        cmd["cell"] = []
        cmd["cell"].append(self._cell(self._bm.cell(col, row)))
        return cmd

    def _cell(self, cell):
        cmd = {}
        cmd["col"] = cell.col
        cmd["row"] = cell.row
        cmd["colour"] = cell.colour.str_rgba
        cmd["border"] = cell.border
        cmd["visible"] = cell.visible
        return cmd
        

bm = BlueMatrix(cols = 2, rows = 1)
c = CommandBuilder(bm)
print(json.dumps(c.all()))
