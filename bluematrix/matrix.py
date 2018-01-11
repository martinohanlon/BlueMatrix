from colours import BLUE
import json

class BlueMatrix():
    """

    """
    def __init__(self, cols = 5, rows = 3, border = True, visible = True, colour = BLUE, bt_device = "hci0", bt_port = 1, auto_start_server = True):
        self._cols = 0
        self._rows = 0
        self._border = border
        self._visible = visible
        self._colour = colour
        self._cells = {}
        self._update_cells(cols, rows)
        
        self._bt_device = bt_device
        self._bt_port = 1
        self._data_buffer = ""

        self._when_client_connects = None
        self._when_client_disconnects = None
        
        self._create_server()

        if auto_start_server:
            self.start()
        
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

    @property
    def running(self):
        """
        Returns a ``True`` if the server is running.
        """
        return self._server.running

    def start(self):
        """
        Start the :class:`.btcomm.BluetoothServer` if it is not already running. By default the server is started at
        initialisation.
        """
        self._server.start()
        self._print_message("Server started {}".format(self.server.server_address))
        self._print_message("Waiting for connection")

    def _create_server(self):
        self._server = BluetoothServer(
                self._data_received,
                when_client_connects = self._client_connected,
                when_client_disconnects = self._client_disconnected,
                device = self._bt_device,
                port = self._bt_port,
                power_up_device = True,
                auto_start = False)

    def stop(self):
        """
        Stop the Bluetooth server.
        """
        self._server.stop()

    def _client_connected(self):
        self._is_connected_event.set()
        self._print_message("Client connected {}".format(self.server.client_address))
        if self.when_client_connects:
            self.when_client_connects()

    def _client_disconnected(self):
        self._is_connected_event.clear()
        self._print_message("Client disconnected")
        if self.when_client_disconnects:
            self.when_client_disconnects()

    def _data_received(self, data):
        #add the data received to the buffer
        self._data_buffer += data

        #get any full commands ended by \n
        last_command = self._data_buffer.rfind("\n")
        if last_command != -1:
            commands = self._data_buffer[:last_command].split("\n")
            #remove the processed commands from the buffer
            self._data_buffer = self._data_buffer[last_command + 1:]
            print(commands)
            #self._process_commands(commands)


    @property
    def when_client_connects(self):
        """
        Sets or returns the function which is called when a Blue Dot connects.
        """
        return self._when_client_connects

    @when_client_connects.setter
    def when_client_connects(self, value):
        self._when_client_connects = value

    @property
    def when_client_disconnects(self):
        """
        Sets or returns the function which is called when a Blue Dot disconnects.
        """
        return self._when_client_disconnects

    @when_client_disconnects.setter
    def when_client_disconnects(self, value):
        self._when_client_disconnects = value

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
        

bm = BlueMatrix(cols = 5, rows = 5)
c = CommandBuilder(bm)
print(json.dumps(c.all()))
