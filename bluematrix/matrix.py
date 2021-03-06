from colours import BLUE, RED, GREEN, WHITE, BLACK, WHITE, YELLOW, Colour
from btcomm import BluetoothServer
from threading import Event
from threads import WrapThread

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
        bt_port = 2, 
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
        self._update_cells(self._cols, value)

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
                uuid = "5c464d54-bb29-4f1e-bcf8-caa0860fb48e",
                port = self._bt_port,
                power_up_device = True,
                auto_start = False)

    def _client_connected(self):
        # send setup data to the client
        self.set_matrix_and_cells()
        
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

    def set_matrix_and_cells(self):
        """
        Sends the whole matrix configuration to the client.
        Called when a new client connects.
        """
        self.set_matrix()
        
        for cell in self.cells:
            if cell.modified:
                self.set_cell(cell)
    
    def set_matrix(self):
        cmd = "1,{},{},{},{},{}\n"
        cmd = cmd.format(
            self.cols,
            self.rows,
            self.colour.str_argb,
            "1" if self.border else "0",
            "1" if self.visible else "0",
        )
        print(cmd)
        self._server.send(cmd)

    def set_cell(self, cell):
        cmd = "2,{},{},{},{},{}\n"
        cmd = cmd.format(
            cell.col,
            cell.row,
            cell.colour.str_argb,
            "1" if cell.border else "0",
            "1" if cell.visible else "0",
        )
        print(cmd)
        self._server.send(cmd)

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

    @property
    def row(self):
        return self._row

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
    
    @property
    def modified(self):
        if self._border == self._matrix.border and self._colour == self._matrix.colour and self._visible == self._matrix.visible:
            return False
        else:
            return True 

    def __str__(self):
        return "({},{}), border = {}, visible = {}, colour = {}".format(self._col, self._row, self._border, self._visible, self._colour)
        
bm = BlueMatrix(cols = 10, rows = 10)
bm.colour = BLACK
bm.border = False
bm.cell(3,3).visible = False
bm.cell(9, 9).colour = RED
bm.cell(4, 4).border = False
bm.cell(9, 9).border = False
bm.cell(1,1).visible = False
bm.cell(4,3).visible = False
bm.cell(6, 7).colour = GREEN
bm.cell(0,1).visible = False
from signal import pause
pause()