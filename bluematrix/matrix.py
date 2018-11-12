from colours import BLUE
from btcomm import BluetoothServer
from threading import Event
import json
from time import sleep

class BlueMatrix(object):
    """

    """
    def __init__(self, 
        cols = 5, 
        rows = 3,
        border = True, 
        visible = True, 
        colour = BLUE, 
        bt_device = "hci0", 
        bt_port = 1, 
        auto_start_server = True,
        print_messages = True):

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
        self._print_messages = print_messages

        self._is_connected_event = Event()
        
        self._when_client_connects = None
        self._when_client_disconnects = None

        self._cb = CommandBuilder(self)
        
        self._create_server()

        if auto_start_server:
            self.start()
            
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
    def server(self):
        """
        The :class:`.btcomm.BluetoothServer` instance that is being used to communicate
        with clients.
        """
        return self._server

    @property
    def running(self):
        """
        Returns a ``True`` if the server is running.
        """
        return self._server.running

    @property
    def print_messages(self):
        """
        When set to ``True`` results in messages relating to the status of the Bluetooth server
        to be printed.
        """
        return self._print_messages

    @print_messages.setter
    def print_messages(self, value):
        self._print_messages = value

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

    # METHODS
    def start(self):
        """
        Start the :class:`.btcomm.BluetoothServer` if it is not already running. By default the server is started at
        initialisation.
        """
        self._server.start()
        self._print_message("Server started {}".format(self.server.server_address))
        self._print_message("Waiting for connection")

    def stop(self):
        """
        Stop the Bluetooth server.
        """
        self._server.stop()

    def cell(self, col, row):
        try:
            return self._cells[(col, row)]
        except KeyError as e:
            raise KeyError("Cell ({},{}) does not exist".format(col, row))

    def resize(self, cols, rows):
        self._update_cells(cols, rows)
        self.server.send(self._cb.all)

    # INTERNAL METHODS
    def _update_cells(self, cols, rows):
        # create new cells
        new_cells = {}

        for c in range(cols):
            for r in range(rows):
                # if cell already exist, reuse it
                if (c,r) in self._cells.keys():
                    new_cells[c,r] = self._cells[(c,r)]
                else:   
                    new_cells[c,r] = BlueMatrixCell(self, c, r, self._border, self._visible, self._colour)
                
        self._cols = cols
        self._rows = rows
        self._cells = new_cells

    def _create_server(self):
        self._server = BluetoothServer(
                self._data_received,
                when_client_connects = self._client_connected,
                when_client_disconnects = self._client_disconnected,
                device = self._bt_device,
                port = self._bt_port,
                power_up_device = True,
                auto_start = False)

    def _client_connected(self):
        # send setup data to the client
        self._send_matrix_config()
        
        self._is_connected_event.set()
        self._print_message("Client connected {}".format(self.server.client_address))
        if self.when_client_connects:
            call_back_t = WrapThread(target=self.when_client_connects)
            call_back_t.start()

    def _client_disconnected(self):
        self._is_connected_event.clear()
        self._print_message("Client disconnected")
        if self.when_client_disconnects:
            call_back_t = WrapThread(target=self.when_client_disconnects)
            call_back_t.start()

    def _data_received(self, data):
        #add the data received to the buffer
        self._data_buffer += data

        #get any full commands ended by \n
        last_command = self._data_buffer.rfind("\n")
        if last_command != -1:
            commands = self._data_buffer[:last_command].split("\n")
            #remove the processed commands from the buffer
            self._data_buffer = self._data_buffer[last_command + 1:]
            print("in = {}".format(commands))
            #self._process_commands(commands)

    def _print_message(self, message):
        if self.print_messages:
            print(message)

    def _send_matrix_config(self):
        """
        Sends the whole matrix configuration to the clients.
        Called when a new client connects.
        """
        self.server.send(self._cb.matrix())
        for cell in self.cells:
            self.server.send(self._cb.cell(cell))
            print(cell)

    def __str__(self):
        return "cols = {}, rows = {}, border = {}, visible = {}, colour = {}".format(self._col, self._row, self._border, self._visible, self._colour)


class BlueMatrixCell(object):
    def __init__(self, matrix, col, row, border, visible, colour):
        self._matrix = matrix
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
        print(self._visible)

    def __str__(self):
        return "({},{}), border = {}, visible = {}, colour = {}".format(self._col, self._row, self._border, self._visible, self._colour)


class CommandBuilder(object):
    
    def __init__(self, bm):
        self._bm = bm

    def matrix(self):
        cmd = "1,{},{}\n".format(self._bm.cols, self._bm.rows)
        # cmd = {}
        # cmd["m"] = {}
        # cmd["m"]["c"] = self._bm.cols
        # cmd["m"]["r"] = self._bm.rows
        return cmd

    def cell(self, cell):
        cmd = "2,{},{},{},{},{}\n"
        cmd = cmd.format(
            cell.col,
            cell.row,
            cell.colour.str_rgba,
            "1" if cell.border else "0",
            "1" if cell.visible else "0",
        )
        # cmd = {}
        # cmd["c"] = {}
        # cmd["c"]["c"] = cell.col
        # cmd["c"]["r"] = cell.row
        # cmd["c"]["col"] = cell.colour.str_rgba
        # cmd["c"]["b"] = "1" if cell.border else "0"
        # cmd["c"]["v"] = "1" if cell.visible else "0"
        return cmd
        
bm = BlueMatrix(cols = 5, rows = 5)
bm.cell(3,3).visible = False
bm.cell(1,1).visible = False
bm.cell(4,3).visible = False
bm.cell(0,1).visible = False
from signal import pause
pause()