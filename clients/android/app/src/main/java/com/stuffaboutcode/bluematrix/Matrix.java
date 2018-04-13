package com.stuffaboutcode.bluematrix;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
// import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MotionEvent;
import android.content.Context;
import android.util.AttributeSet;
import android.content.res.TypedArray;
import android.graphics.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.Math;
import java.util.UUID;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

class DynamicMatrix extends View {

    private ArrayList<ArrayList<MatrixCell>> mCells;
    private int mCols, mRows;
    private Paint mTextPaint, mCellPaint, mBorderPaint, mLinePaint;
    private float mTextHeight;
    private int mWidth, mHeight;
    int mMatrixWidth, mMatrixHeight;
    int mCellSize;
    private Context mContext;
    private RectF mMatrixBounds = new RectF();
    //private MatrixCell mPressedCell = null, mMovedToCell = null;

    /*private boolean mRestoreState = false;
    private int[] mColorsState;
    private boolean[] mVisibleState;
    private boolean[] mBordersState;*/

    private HashMap<Integer, MatrixPointer> pointers = new HashMap<Integer, MatrixPointer>();

    public DynamicMatrix(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.DynamicMatrix,
                0, 0);

        try {
            mCols = a.getInteger(R.styleable.DynamicMatrix_cols, 0);
            mRows = a.getInteger(R.styleable.DynamicMatrix_rows, 0);
        } finally {
            a.recycle();
        }

        init();
    }

    private void init() {

        /*mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(context.getResources().getColor(R.color.white));
        if (mTextHeight == 0) {
            mTextHeight = mTextPaint.getTextSize();
        } else {
            mTextPaint.setTextSize(mTextHeight);
        }*/

        mCellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCellPaint.setStyle(Paint.Style.FILL);

        mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(5);
        mBorderPaint.setColor(mContext.getResources().getColor(R.color.darkgrey));

        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setStrokeWidth(5);

        setupMatrix();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int xpad = (getPaddingLeft() + getPaddingRight());
        int ypad = (getPaddingTop() + getPaddingBottom());

        mWidth = w - xpad;
        mHeight = h - ypad;

        sizeMatrix();
    }

    private void setupMatrix() {
        // setup matrix is called on init or when the number of rows or cols changes
        // and creates a default matrix with a zero size, its not sized until onSizeChanged
        // is called

        mMatrixWidth = 0;
        mMatrixHeight = 0;
        mCellSize = 0;
        mMatrixBounds = new RectF(0 ,0,0,0);
                // create the cells
        mCells = new ArrayList<ArrayList<MatrixCell>>();
        int color = mContext.getResources().getColor(R.color.defaultCellColor);

        for(int c = 0; c < getCols(); c++) {
            mCells.add(new ArrayList<MatrixCell>());
            for (int r = 0; r < getRows(); r++) {
                mCells.get(c).add(new MatrixCell(
                        c,
                        r,
                        new RectF(0,0,0,0),
                        true,
                        true,
                        color));
            }
        }
    }

    private void sizeMatrix() {
        // called when the screen size of the matrix needs to change

        // calc potential size of matrix
        int left = 0, top = 0;

        // find out how big each cell can be
        if ((mWidth / getCols()) < (mHeight/ getRows())) {
            mCellSize = (int)(mWidth / getCols());
            mMatrixWidth = mWidth;
            mMatrixHeight = getRows() * mCellSize;
            top = (mHeight - mMatrixHeight) / 2;
        } else {
            mCellSize = (int)(mHeight / getRows());
            mMatrixWidth = getCols() * mCellSize;
            left = (mWidth - mMatrixWidth) / 2;
            mMatrixHeight = mHeight;
        }

        // create the bounds for the matrix
        mMatrixBounds = new RectF(
                left,
                top,
                left + mMatrixWidth,
                top + mMatrixHeight);

        for(int c = 0; c < getCols(); c++) {
            for (int r = 0; r < getRows(); r++) {
                mCells.get(c).get(r).setBounds(sizeCell(c, r));
            }
        }

        // set line thickness
        mBorderPaint.setStrokeWidth((float)Math.max(1, mCellSize * 0.025));
        mLinePaint.setStrokeWidth((float)Math.max(5, mCellSize * 0.025));

    }

    private RectF sizeCell(int c, int r) {
        return new RectF(
                (int) mMatrixBounds.left + (c * mCellSize),
                (int) mMatrixBounds.top + (r * mCellSize),
                (int) mMatrixBounds.left + (c * mCellSize) + mCellSize,
                (int) mMatrixBounds.top + (r * mCellSize) + mCellSize);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (ArrayList<MatrixCell> row : mCells) {
            for (MatrixCell cell : row ) {
                if (cell.getVisible()) mCellPaint.setColor(cell.getColor());
                else mCellPaint.setColor(Color.TRANSPARENT);
                canvas.drawRect(cell.getBounds(), mCellPaint);
            }
        }

        for (ArrayList<MatrixCell> row : mCells) {
            for (MatrixCell cell : row ) {
                if (cell.getVisible() && cell.getBorder()) {
                    canvas.drawRect(cell.getBounds(), mBorderPaint);
                }
            }
        }

        /*for (Integer pointerId : pointers.keySet()){
            MatrixPointer pointer = pointers.get(pointerId);

            float x = pointer.getX();
            float y = pointer.getY();
            RectF cellBounds = pointer.getPressedCell().getBounds();
            float highlightWidth = cellBounds.width() * 0.1f;

            //draw a line from the centre of the cell to the position
            mLinePaint.setColor(pointer.getPressedCell().getMovedColor());
            canvas.drawLine(cellBounds.centerX(), cellBounds.centerY(), x, y, mLinePaint);

            // is pointer inside the pressed cell?
            //if (cellBounds.contains(x,y)){
            //    RectF selectedRect = new RectF(x - highlightWidth, y - highlightWidth, x + highlightWidth, y + highlightWidth);
            //    mCellPaint.setColor(pointer.getPressedCell().getMovedColor());
            //    canvas.drawRect(selectedRect, mCellPaint);
            //}

        }*/
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int pointerIndex, pointerId;
        MatrixPointer pointer;
        MatrixCell cell;
        float x, y;

        switch(event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
                // do the perform click
                performClick();

            case MotionEvent.ACTION_POINTER_DOWN:

                // TODO have a look at the acceleration bit
                pointerIndex = event.getActionIndex();
                x = event.getX(pointerIndex);
                y = event.getY(pointerIndex);

                // was it inside the matrix?
                if (mMatrixBounds.contains(x, y)) {
                    cell = findCellFromXY(x, y);

                    pointerId = event.getPointerId(pointerIndex);
                    pointers.put(pointerId, new MatrixPointer(pointerId, x, y, cell));

                    cell.press();
                }
                break;

            case MotionEvent.ACTION_UP:

            case MotionEvent.ACTION_POINTER_UP:
                pointerIndex = event.getActionIndex();
                pointerId = event.getPointerId(pointerIndex);
                pointer = pointers.get(pointerId);
                if (pointer != null) {
                    pointer.getPressedCell().release();
                    pointers.remove(pointerId);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                pointerIndex = event.getActionIndex();
                x = event.getX(pointerIndex);
                y = event.getY(pointerIndex);

                // was it inside the matrix?
                if (mMatrixBounds.contains(x, y)) {
                    // was it inside the pressed cell?
                    pointerId = event.getPointerId(pointerIndex);
                    pointer = pointers.get(pointerId);
                    if (pointer != null) {
                        pointer.move(x, y);
                        //pointer.getPressedCell().moved();
                    }
                }
                break;

        }
        return true;
    }

    private MatrixCell findCellFromXY(float x, float y) {
        /*for (ArrayList<MatrixCell> row : mCells) {
            for (MatrixCell cell : row) {
                if (cell.getBounds().contains(x, y)) {
                    return cell
                }
            }
        }*/
        int col = (int)(x - mMatrixBounds.left) / mCellSize;
        int row = (int)(y - mMatrixBounds.top) / mCellSize;
        return mCells.get(col).get(row);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    public void update() {
        // updates the matrix, must be called after each update to the matrix to display the changes
        invalidate();
        requestLayout();
    }

    // getters and setters
    public ArrayList<ArrayList<MatrixCell>> getCells() {
        return mCells;
    }

    public MatrixCell getCell(int col, int row) {
        return mCells.get(col).get(row);
    }

    public void setSize(int cols, int rows) {
        cols = Math.max(1, cols);
        rows = Math.max(1, rows);
        mCols = cols;
        mRows = rows;
        setupMatrix();
        sizeMatrix();

    }

    public int getCols() {
        return mCols;
    }

    public void setCols(int value) {
        setSize(value, getRows());
    }

    public int getRows() {
        return mRows;
    }

    public void setRows(int value) {
        setSize(getCols(), value);
    }

    // internal classes
    private class MatrixPointer {

        private int mPointedId;
        private float mX, mY;
        private MatrixCell mPressedCell;

        private MatrixPointer(int pointerId, float x, float y, MatrixCell pressedCell) {
            mPointedId = pointerId;
            mX = x;
            mY = y;
            mPressedCell = pressedCell;
        }
        private void move(float x, float y) {
            mX = x;
            mY = y;
        }
        private MatrixCell getPressedCell() {
            return mPressedCell;
        }
        private float getX() {
            return mX;
        }
        private float getY() {
            return mY;
        }
    }

    public class MatrixCell {

        private int mRow, mCol, mCurrentColor, mReleasedColor, mPressedColor, mMovedColor;
        private RectF mBounds;
        private boolean mBorder, mPressed, mVisible;

        private MatrixCell(int col, int row, RectF bounds, boolean visible, boolean border, int color) {
            mCol = col;
            mRow = row;
            mBounds = bounds;
            mBorder = border;
            mPressed = false;
            mVisible = visible;
            updateColors(color);
        }
        /*public int getRow() { return mRow; }
        public int getCol() { return mCol; }*/
        private RectF getBounds() { return mBounds; }
        private void setBounds(RectF value) {
            mBounds = value;
        }
        private int getColor() {
            return mCurrentColor;
        }
        private int getMovedColor() {
            return mMovedColor;
        }
        public void setColor(int value) {
            updateColors(value);
        }
        private boolean getBorder() {
            return mBorder;
        }
        public void setBorder(boolean value) {
            mBorder = value;
        }
        public boolean getVisible() {
            return mVisible;
        }
        public void setVisible(boolean value) {
            mVisible = value;
        }
        private void press() {
            mCurrentColor = mPressedColor;
            mPressed = true;
            invalidate();
            requestLayout();
        }
        private void release() {
            mCurrentColor = mReleasedColor;
            mPressed = false;
            invalidate();
            requestLayout();
        }
        private void moved() {
            invalidate();
            requestLayout();
        }
        private void updateColors(int color) {
            mReleasedColor = color;
            mPressedColor = manipulateColor(color, 0.85f);
            mMovedColor = manipulateColor(color, 0.7f);

            if (mPressed) {
                mCurrentColor = mPressedColor;
            } else {
                mCurrentColor = mReleasedColor;
            }
        }

        private int manipulateColor(int color, float factor) {
            int a = Color.alpha(color);
            int r = Math.round(Color.red(color) * factor);
            int g = Math.round(Color.green(color) * factor);
            int b = Math.round(Color.blue(color) * factor);
            return Color.argb(a,
                    Math.min(r,255),
                    Math.min(g,255),
                    Math.min(b,255));
        }
    }
}

public class Matrix extends AppCompatActivity {


    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;

    String address = null;
    String deviceName = null;

    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    private boolean connectionLost = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private ProgressDialog progress;

    TextView statusView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_matrix);

        statusView = (TextView)findViewById(R.id.status);

        final DynamicMatrix matrix = findViewById(R.id.matrix);

        Intent newint = getIntent();

        deviceName = newint.getStringExtra(Connect.EXTRA_NAME);
        address = newint.getStringExtra(Connect.EXTRA_ADDRESS);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_LONG).show();
            this.finish();
        }

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");

        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, true);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sendMessage("hi");

        /*final Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final DynamicMatrix matrix = findViewById(R.id.matrix);
                //matrix.setCols(matrix.getCols() + 1);
                //matrix.setRows(matrix.getRows() + 1);
                //matrix.getCell(1,0).setColor(Color.rgb(255,0,0));
                matrix.setSize(6, 3);
                matrix.getCell(0,0).setVisible(false);
                matrix.getCell(0,2).setVisible(false);
                matrix.getCell(2,0).setVisible(false);
                matrix.getCell(2,2).setVisible(false);
                matrix.getCell(3,0).setVisible(false);
                matrix.getCell(3,1).setVisible(false);
                matrix.getCell(3,2).setVisible(false);
                matrix.getCell(3,2).setVisible(false);
                matrix.getCell(4,0).setVisible(false);
                matrix.getCell(5,0).setVisible(false);
                matrix.getCell(4,2).setVisible(false);
                matrix.getCell(5,2).setVisible(false);
                matrix.getCell(4,1).setColor(Color.rgb(255,0,0));
                matrix.getCell(5,1).setColor(Color.rgb(0,255,0));
                matrix.getCell(4,1).setBorder(false);
                matrix.getCell(5,1).setBorder(false);
                matrix.update();
            }
        });*/
    }

    public void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, "cant send message - not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
        }
    }

    private void disconnect() {
        if (mChatService != null) {
            mChatService.stop();
        };
        finish();
    }

    @Override
    public void onBackPressed() {
        disconnect();
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            statusView.setText("Connected to " + deviceName);
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            statusView.setText("Connecting to " + deviceName);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            statusView.setText("Not connected");
                            disconnect();
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    // message sent
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    // message received
                    statusView.setText(readMessage);
                    //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != this) {
                        Toast.makeText(getApplicationContext(), "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != this) {
                        Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }

        }
    };

}


/* potential messages the matrix might receive

messages:
- matrix
    - cols
    - rows
- cell [array]
    - col
    - row
    - colour
    - border
    - visible

- all - reconfiguration of the matrix and updated values for all the cells


 */