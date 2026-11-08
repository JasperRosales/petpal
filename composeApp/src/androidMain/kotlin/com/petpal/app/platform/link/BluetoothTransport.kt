package com.petpal.app.platform.link

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.os.Build
import com.petpal.app.domain.platform.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.util.UUID

/** Battles over Bluetooth Classic (RFCOMM). Permissions are requested by the UI first. */
@SuppressLint("MissingPermission")
class BluetoothTransport(private val context: Context) : BattleTransport {
    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?)?.adapter

    override val kind = LinkKind.BLUETOOTH
    override val isAvailable get() = adapter != null

    private fun requireOn(): BluetoothAdapter {
        val a = adapter ?: throw IOException("No Bluetooth on this device")
        if (!a.isEnabled) throw IOException("Turn on Bluetooth first")
        return a
    }

    override suspend fun host(name: String): BattleConnection {
        val a = requireOn()
        // Let unpaired phones find us for 2 minutes.
        runCatching {
            context.startActivity(
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                    .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        val server = a.listenUsingInsecureRfcommWithServiceRecord("PetPal Battle", SERVICE_UUID)
        try {
            val socket = blockingIo(server) { server.accept() }
            return StreamConnection(socket.inputStream, socket.outputStream, socket)
        } finally {
            runCatching { server.close() }
        }
    }

    override fun discover(): Flow<List<Peer>> = callbackFlow {
        val a = requireOn()
        val found = LinkedHashMap<String, Peer>()
        fun label(d: BluetoothDevice) = runCatching { d.name }.getOrNull() ?: d.address
        a.bondedDevices.orEmpty().forEach { found[it.address] = Peer(it.address, "${label(it)} (paired)") }
        trySend(found.values.toList())

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val d = if (Build.VERSION.SDK_INT >= 33)
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        else @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        if (d != null && d.address !in found) {
                            found[d.address] = Peer(d.address, label(d))
                            trySend(found.values.toList())
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> a.startDiscovery() // keep scanning
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        else context.registerReceiver(receiver, filter)
        a.startDiscovery()
        awaitClose {
            runCatching { a.cancelDiscovery() }
            context.unregisterReceiver(receiver)
        }
    }

    override suspend fun connect(peer: Peer): BattleConnection {
        val a = requireOn()
        a.cancelDiscovery() // discovery slows connections down a lot
        val socket = a.getRemoteDevice(peer.id).createInsecureRfcommSocketToServiceRecord(SERVICE_UUID)
        blockingIo(socket) { socket.connect() }
        return StreamConnection(socket.inputStream, socket.outputStream, socket)
    }

    private companion object {
        val SERVICE_UUID: UUID = UUID.fromString("6f1d2a8e-4b8c-4d6e-9a51-7e0b5c2f9a11")
    }
}
